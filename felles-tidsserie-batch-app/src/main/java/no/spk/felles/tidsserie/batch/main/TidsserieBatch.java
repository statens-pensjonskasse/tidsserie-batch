package no.spk.felles.tidsserie.batch.main;

import static java.time.Duration.between;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static no.spk.felles.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PREFIX;
import static no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus.onStop;
import static no.spk.felles.tidsserie.batch.core.registry.Ranking.ranking;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.timeout.BatchTimeout;
import no.spk.faktura.timeout.BatchTimeoutTaskrunner;
import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklusException;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.GrunnlagsdataRepository;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.felles.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.felles.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.felles.tidsserie.batch.main.spi.ExitCommand;
import no.spk.felles.tidsserie.batch.storage.disruptor.FileTemplate;
import no.spk.felles.tidsserie.batch.storage.disruptor.LmaxDisruptorPublisher;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Platformrammeverk for generering av tidsseriar ved hjelp av felles-tidsperiode-underlag-lib og plugbare {@link Tidsseriemodus modusar}.
 * <br>
 * Batchen er ansvarlig for å laste inn grunnlagsdata som modusane kan benytte for å bygge opp tidsseriar. Datafilene den leser inn blir lasta opp til
 * ein Hazelcast-basert in-memory backend som modusane kan hente ut grunnlagsdata frå via {@link GrunnlagsdataRepository}.
 * <br>
 * Ved køyring av batchen må brukaren angi kva {@link Tidsseriemodus modus} som skal benyttast og som følge av dette valget, kva type tidsserie
 * som skal bli generert.
 *
 * @author Snorre E. Brekke - Computas
 * @author Tarjei Skorgenes
 */
public class TidsserieBatch {
    private final Extensionpoint<TidsserieLivssyklus> livssyklus;
    private final Extensionpoint<TidsserieGenerertCallback> generert;
    private final ServiceRegistry registry;
    private final ExitCommand exiter;

    private ApplicationController controller;

    /**
     * Konstruerer ein ny instans av platformrammeverket som skal brukast til å eksekvere batchen.
     * <br>
     * For å støtte direkte kall til batchen frå integrasjonstestar er {@code exiter}
     * lagt til som eit parameter ved konstruksjon. Dette for å unngå
     * direkte kall til {@link System#exit(int)} som dreper test-JVMen.
     *
     * @param registry tjenesteregisteret som alle tjenester brukt av batchen skal registrerast i og hentast frå
     * @param exiter konsument som tar seg av å avslutte batchkøyringa ved kall til {@link #shutdown()}
     * @param controller kontrolleren som tar seg av å informere brukaren, logging og handtering av exitkode
     * @throws NullPointerException dersom nokon av parameterverdiane er lik {@code null}
     */
    public TidsserieBatch(final ServiceRegistry registry, final ExitCommand exiter,
                          final ApplicationController controller) {
        this.registry = requireNonNull(registry, "registry er påkrevd, men var null");
        this.exiter = requireNonNull(exiter, "exiter er påkrevd, men var null");
        this.controller = requireNonNull(controller, "controller er påkrevd, men var null");
        this.livssyklus = new Extensionpoint<>(TidsserieLivssyklus.class, registry);
        this.generert = new Extensionpoint<>(TidsserieGenerertCallback.class, registry);
    }

    public void run(final Supplier<TidsserieBatchArgumenterParser> parser, final String... args) {
        try {
            final TidsserieBatchArgumenter arguments = parser.get().parse(args);
            registrer(TidsserieBatchArgumenter.class, arguments);
            arguments.registrer(registry);

            startBatchTimeout(arguments);

            final BatchId batchId = new BatchId(TIDSSERIE_PREFIX, now());

            final Path logKatalog = batchId.tilArbeidskatalog(arguments.logkatalog());
            final Path utKatalog = arguments.utkatalog().resolve("tidsserie");
            final Path innKatalog = arguments.uttrekkskatalog();

            registrer(Path.class, logKatalog, Katalog.LOG.egenskap());
            registrer(Path.class, utKatalog, Katalog.UT.egenskap());
            registrer(Path.class, innKatalog, Katalog.GRUNNLAGSDATA.egenskap());

            Files.createDirectories(logKatalog);

            controller.initialiserLogging(batchId, logKatalog);
            controller.informerOmOppstart(arguments);

            final DirectoryCleaner directoryCleaner = createDirectoryCleaner(
                    arguments.slettegrense(),
                    arguments.logkatalog(),
                    utKatalog
            );
            registrer(DirectoryCleaner.class, directoryCleaner);
            controller.ryddOpp(directoryCleaner);
            Files.createDirectories(utKatalog);

            registrer(GrunnlagsdataDirectoryValidator.class, new ChecksumValideringAvGrunnlagsdata(innKatalog));

            final Tidsseriemodus modus = arguments.modus();
            registrer(Tidsseriemodus.class, modus);

            final MetaDataWriter metaDataWriter = new MetaDataWriter(TemplateConfigurationFactory.create(), logKatalog);
            registrer(MetaDataWriter.class, metaDataWriter);

            final ExecutorService executors = newCachedThreadPool(
                    r -> new Thread(r, "lmax-disruptor-" + System.currentTimeMillis())
            );
            registrer(ExecutorService.class, executors);
            registrer(TidsserieLivssyklus.class, onStop(executors::shutdown));

            final LmaxDisruptorPublisher disruptor = new LmaxDisruptorPublisher(
                    executors,
                    new FileTemplate(utKatalog, ".csv")
            );
            registrer(StorageBackend.class, disruptor);
            registrer(TidsserieLivssyklus.class, disruptor);

            controller.aktiverPlugins();
            modus.registerServices(registry);
            registrer(TidsserieGenerertCallback.class, new TriggerfileCreator(utKatalog), ranking(1000).egenskap());

            final LocalDateTime started = now();
            controller.validerGrunnlagsdata();
            controller.startBackend();
            controller.lastOpp();

            lagTidsserie(
                    controller,
                    modus
            );
            controller.opprettMetadata(metaDataWriter, arguments, batchId, between(started, now()));

            controller.informerOmSuksess(logKatalog);
        } catch (UgyldigKommandolinjeArgumentException e) {
            controller.informerOmUgyldigeArgumenter(e);
        } catch (BruksveiledningSkalVisesException e) {
            controller.informerOmBruk(e);
        } catch (GrunnlagsdataException e) {
            controller.informerOmKorrupteGrunnlagsdata(e);
        } catch (HousekeepingException e) {
            controller.informerOmFeiletOpprydding();
        } catch (final Exception e) {
            controller.informerOmUkjentFeil(e);
        }

        shutdown();
    }

    void lagTidsserie(
            final ApplicationController controller,
            final Tidsseriemodus modus
    ) {
        try {
            livssyklus
                    .invokeAll(l -> l.start(registry))
                    .orElseThrow(TidsserieBatch::livssyklusStartFeila);

            controller.lagTidsserie(
                    registry,
                    modus
            );

            generert
                    .invokeAll(g -> g.tidsserieGenerert(registry))
                    .orElseThrow(TidsserieGenerertException::new);
        } finally {
            // Unngå at feil ved stop sluker eventuelle feil som boblar ut av tidsseriegenereringa
            livssyklus
                    .invokeAll(l -> l.stop(registry))
                    .forEachFailure(controller::informerOmUkjentFeil);
        }
    }

    private <T> ServiceRegistration<T> registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        return registry.registerService(type, tjeneste, egenskapar);
    }

    private DirectoryCleaner createDirectoryCleaner(
            final AldersgrenseForSlettingAvLogKatalogar grense,
            final Path logKatalog,
            final Path dataKatalog
    ) throws HousekeepingException {
        DeleteBatchDirectoryFinder finder = new DeleteBatchDirectoryFinder(dataKatalog, logKatalog);
        Path[] deleteDirectories = finder.findDeletableBatchDirectories(grense);
        return new DirectoryCleaner(deleteDirectories);
    }

    private void shutdown() {
        controller.logExit();
        exiter.exit(controller.exitCode());
    }

    private void startBatchTimeout(TidsserieBatchArgumenter arguments) {
        new BatchTimeoutTaskrunner(
                createBatchTimeout(arguments)).startTerminationTimeout
                (
                        ofMinutes(0),
                        () -> {
                            controller.logTimeout();
                            shutdown();
                        }
                );
    }

    private BatchTimeout createBatchTimeout(TidsserieBatchArgumenter arguments) {
        return new BatchTimeout(
                arguments.maksimalKjøretid(),
                arguments.avsluttFørTidspunkt()
        )
                .start();
    }

    private static TidsserieLivssyklusException livssyklusStartFeila(final Stream<RuntimeException> errors) {
        return new TidsserieLivssyklusException("start", errors);
    }

}
