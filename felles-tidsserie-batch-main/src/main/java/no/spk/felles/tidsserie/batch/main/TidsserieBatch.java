package no.spk.felles.tidsserie.batch.main;

import static java.time.Duration.between;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;
import static java.util.Objects.requireNonNull;
import static no.spk.felles.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PREFIX;
import static no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback2.Metadata;
import static no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback2.metadata;
import static no.spk.felles.tidsserie.batch.core.registry.ExtensionpointStatus.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.timeout.BatchTimeout;
import no.spk.faktura.timeout.BatchTimeoutTaskrunner;
import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklusException;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.GrunnlagsdataRepository;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.felles.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.felles.tidsserie.batch.main.spi.ExitCommand;
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
    private final Extensionpoint<TidsserieGenerertCallback2> generert;
    @SuppressWarnings("deprecation")
    private final Extensionpoint<no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback> generertOld;
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
    @SuppressWarnings("deprecation")
    public TidsserieBatch(final ServiceRegistry registry, final ExitCommand exiter,
                          final ApplicationController controller) {
        this.registry = requireNonNull(registry, "registry er påkrevd, men var null");
        this.exiter = requireNonNull(exiter, "exiter er påkrevd, men var null");
        this.controller = requireNonNull(controller, "controller er påkrevd, men var null");
        this.livssyklus = new Extensionpoint<>(TidsserieLivssyklus.class, registry);
        this.generert = new Extensionpoint<>(TidsserieGenerertCallback2.class, registry);
        this.generertOld = new Extensionpoint<>(
                no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback.class,
                registry
        );
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

            final Tidsseriemodus modus = arguments.modus();
            registrer(Tidsseriemodus.class, modus);

            final DirectoryCleaner directoryCleaner = createDirectoryCleaner(
                    arguments.slettegrense(),
                    arguments.logkatalog(),
                    utKatalog
            );
            registrer(DirectoryCleaner.class, directoryCleaner);
            controller.ryddOpp(directoryCleaner);
            Files.createDirectories(utKatalog);

            controller.aktiverPlugins();
            modus.registerServices(registry);

            final LocalDateTime started = now();
            controller.validerGrunnlagsdata();
            controller.startBackend();
            controller.lastOpp();

            lagTidsserie(
                    controller,
                    modus,
                    started,
                    batchId
            );

            controller.informerOmSuksess(logKatalog);
        } catch (UgyldigKommandolinjeArgumentException e) {
            controller.informerOmUgyldigeArgumenter(e);
        } catch (BruksveiledningSkalVisesException e) {
            controller.informerOmBruk(e);
        } catch (UgyldigUttrekkException e) {
            controller.informerOmKorrupteGrunnlagsdata(e);
        } catch (HousekeepingException e) {
            controller.informerOmFeiletOpprydding();
        } catch (final Exception e) {
            controller.informerOmUkjentFeil(e);
        }

        shutdown();
    }

    @SuppressWarnings("deprecation")
    void lagTidsserie(
            final ApplicationController controller,
            final Tidsseriemodus modus,
            final LocalDateTime start,
            final BatchId kjøring
    ) {
        try {
            livssyklus
                    .invokeAll(l -> l.start(registry))
                    .orElseThrow(TidsserieBatch::livssyklusStartFeila);

            controller.lagTidsserie(
                    registry,
                    modus
            );

            final Metadata metadata = metadata(kjøring, between(now(), start));
            merge(
                    generertOld.invokeAll(g -> g.tidsserieGenerert(registry)),
                    generert.invokeAll(g -> g.tidsserieGenerert(registry, metadata))
            )
                    .orElseThrow(TidsserieGenerertException::new);
        } finally {
            // Unngå at feil ved stop sluker eventuelle feil som boblar ut av tidsseriegenereringa
            livssyklus
                    .invokeAll(l -> l.stop(registry))
                    .forEachFailure(controller::informerOmUkjentFeil);
        }
    }

    private <T> void registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        registry.registerService(type, tjeneste, egenskapar);
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
