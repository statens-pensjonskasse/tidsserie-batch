package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.Duration.between;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.TIDSSERIE_PREFIX;
import static no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus.onStop;
import static no.spk.pensjon.faktura.tidsserie.util.Services.lookupAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.faktura.timeout.BatchTimeout;
import no.spk.faktura.timeout.BatchTimeoutTaskrunner;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.TidsserieArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor.LmaxDisruptorPublisher;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.upload.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.util.Services;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Batch som genererer tidsseriar for forenkla fakturering fastsats.
 * <br>
 * Batchen er avhengig av datasett generert av faktura-grunnlagsdata-batch. Datafilene den genererer blir lest inn av
 * faktura-tidsserie-batch, lasta opp til ein Hazelcast-basert in-memory backend og deretter brukt for å
 * generere tidsseriar på stillingsforholdnivå pr premieår pr observasjonsdato.
 * <br>
 *
 * @author Snorre E. Brekke - Computas
 * @author Tarjei Skorgenes
 */
public class TidsserieMain {
    private final ServiceRegistry registry;
    private final Consumer<Integer> exiter;

    private ApplicationController controller;

    /**
     * Konstruerer ein ny instans av main-klassa som skal brukast til å eksekvere batchen.
     * <br>
     * For å støtte direkte kall til batchen frå integrasjonstestar er {@code exiter} lagt til som eit parameter ved konstruksjon. Dette for å unngå
     * direkte kall til {@link System#exit(int)} som dreper test-JVMen.
     *
     * @param registry tjenesteregisteret som alle tjenester brukt av batchen skal registrerast i og hentast frå
     * @param exiter konsument som tar seg av å avslutte batchkøyringa ved kall til {@link #shutdown()}
     * @throws NullPointerException dersom nokon av parameterverdiane er lik {@code null}
     */
    public TidsserieMain(final ServiceRegistry registry, final Consumer<Integer> exiter) {
        this.registry = requireNonNull(registry, "registry er påkrevd, men var null");
        this.exiter = requireNonNull(exiter, "exiter er påkrevd, men var null");
    }

    public void run(final String... args) {
        controller = new ApplicationController(new ConsoleView());

        try {
            ProgramArguments arguments = new TidsserieArgumentsFactory().create(args);
            startBatchTimeout(arguments);

            final BatchId batchId = new BatchId(TIDSSERIE_PREFIX, now());
            Path logKatalog = batchId.tilArbeidskatalog(arguments.getLogkatalog());
            Path utKatalog = arguments.getUtkatalog().resolve("tidsserie");

            registrer(Path.class, logKatalog, Katalog.LOG.egenskap());
            registrer(Path.class, utKatalog, Katalog.UT.egenskap());
            registrer(Path.class, arguments.getGrunnlagsdataBatchKatalog(), Katalog.GRUNNLAGSDATA.egenskap());

            registrer(Observasjonsperiode.class, arguments.observasjonsperiode());

            Files.createDirectories(logKatalog);

            controller.initialiserLogging(batchId, logKatalog);
            controller.informerOmOppstart(arguments);

            final DirectoryCleaner directoryCleaner = createDirectoryCleaner(arguments.getSlettLogEldreEnn(), arguments.getLogkatalog(), utKatalog);
            registrer(DirectoryCleaner.class, directoryCleaner);
            controller.ryddOpp(directoryCleaner);
            Files.createDirectories(utKatalog);

            registrer(GrunnlagsdataDirectoryValidator.class, new ChecksumValideringAvGrunnlagsdata(arguments.getGrunnlagsdataBatchKatalog()));

            final Tidsseriemodus modus = arguments.modus();
            registrer(Tidsseriemodus.class, modus);

            final TidsserieBackendService backend = new HazelcastBackend(registry, arguments.getNodes());
            registrer(TidsserieBackendService.class, backend);

            final GrunnlagsdataRepository input = modus.repository(arguments.getInnkatalog().resolve(arguments.getGrunnlagsdataBatchId()));
            registrer(GrunnlagsdataRepository.class, input);

            final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);
            registrer(GrunnlagsdataService.class, overfoering);
            registrer(TidsserieFactory.class, overfoering);
            registrer(TidsperiodeFactory.class, overfoering);

            final MetaDataWriter metaDataWriter = new MetaDataWriter(TemplateConfigurationFactory.create(), logKatalog);
            registrer(MetaDataWriter.class, metaDataWriter);

            final ExecutorService executors = newCachedThreadPool(
                    r -> new Thread(r, "lmax-disruptor-" + System.currentTimeMillis())
            );
            registrer(ExecutorService.class, executors);
            registrer(TidsserieLivssyklus.class, onStop(executors::shutdown));

            final LmaxDisruptorPublisher disruptor = new LmaxDisruptorPublisher(
                    executors,
                    new FileTemplate(utKatalog, "tidsserie", ".csv")
            );
            registrer(StorageBackend.class, disruptor);
            registrer(TidsserieLivssyklus.class, disruptor);

            modus.registerServices(registry);

            final LocalDateTime started = now();
            controller.validerGrunnlagsdata(Services.lookup(registry, GrunnlagsdataDirectoryValidator.class));
            controller.startBackend(backend);
            controller.lastOpp(overfoering);

            all(TidsserieLivssyklus.class).forEach((l -> l.start(registry)));

            controller.lagTidsserie(
                    registry,
                    modus,
                    arguments.observasjonsperiode()
            );

            all(TidsserieLivssyklus.class).forEach((l -> l.stop(registry)));

            controller.opprettMetadata(metaDataWriter, arguments, batchId, between(started, now()));

            controller.informerOmSuksess(logKatalog);
        } catch (InvalidParameterException e) {
            controller.informerOmUgyldigeArgumenter(e);
        } catch (UsageRequestedException e) {
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

    private <T> Stream<T> all(Class<T> type) {
        return lookupAll(registry, type);
    }

    public static void main(String[] args) {
        final Consumer<Integer> exiter = System::exit;
        new TidsserieMain(
                ServiceLoader.load(ServiceRegistry.class)
                        .iterator()
                        .next(),
                exiter
        )
                .run(args);
    }

    private <T> ServiceRegistration<T> registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        return registry.registerService(type, tjeneste, egenskapar);
    }

    private DirectoryCleaner createDirectoryCleaner(int slettEldreEnn, Path logKatalog, Path dataKatalog) throws HousekeepingException {
        DeleteBatchDirectoryFinder finder = new DeleteBatchDirectoryFinder(dataKatalog, logKatalog);
        Path[] deleteDirectories = finder.findDeletableBatchDirectories(slettEldreEnn);
        return new DirectoryCleaner(deleteDirectories);
    }

    private void shutdown() {
        controller.logExit();
        exiter.accept(controller.exitCode());
    }

    private void startBatchTimeout(ProgramArguments arguments) {
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

    private BatchTimeout createBatchTimeout(ProgramArguments arguments) {
        return new BatchTimeout(arguments.getKjoeretid(), arguments.getSluttidspunkt()).start();
    }

}
