package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.Duration.between;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.TIDSSERIE_PREFIX;
import static no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus.onStart;
import static no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus.onStop;
import static no.spk.pensjon.faktura.tidsserie.core.TidsserieResulat.tidsserieResulat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
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
import no.spk.pensjon.faktura.tidsserie.core.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
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
    private final static ServiceRegistry REGISTRY = ServiceLoader.load(ServiceRegistry.class)
            .iterator()
            .next();

    public static void main(String[] args) {
        final ApplicationController controller = new ApplicationController(new ConsoleView());

        try {
            ProgramArguments arguments = new TidsserieArgumentsFactory().create(args);
            startBatchTimeout(arguments, controller);

            final BatchId batchId = new BatchId(TIDSSERIE_PREFIX, now());
            Path logKatalog = batchId.tilArbeidskatalog(arguments.getLogkatalog());
            Path utKatalog = arguments.getUtkatalog().resolve("tidsserie");

            registrer(Path.class, logKatalog, "katalog=log");
            registrer(Path.class, utKatalog, "katalog=ut");
            registrer(Path.class, arguments.getGrunnlagsdataBatchKatalog(), "katalog=grunnlagsdata");

            registrer(Observasjonsperiode.class, arguments.observasjonsperiode());

            Files.createDirectories(logKatalog);

            controller.initialiserLogging(batchId, logKatalog);
            controller.informerOmOppstart(arguments);

            final DirectoryCleaner directoryCleaner = createDirectoryCleaner(arguments.getSlettLogEldreEnn(), arguments.getLogkatalog(), utKatalog);
            registrer(DirectoryCleaner.class, directoryCleaner);
            controller.ryddOpp(directoryCleaner);
            Files.createDirectories(utKatalog);

            final GrunnlagsdataDirectoryValidator grunnlagsdataValidator = new GrunnlagsdataDirectoryValidator(arguments.getGrunnlagsdataBatchKatalog());
            registrer(GrunnlagsdataDirectoryValidator.class, grunnlagsdataValidator);

            final Tidsseriemodus modus = arguments.modus();
            registrer(Tidsseriemodus.class, modus);
            registrer(TidsserieLivssyklus.class, onStop(() -> modus.completed(tidsserieResulat(utKatalog).bygg())));

            final TidsserieBackendService backend = new HazelcastBackend(REGISTRY, arguments.getNodes());
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
            registrer(TidsserieLivssyklus.class, onStart(() -> modus.initStorage(disruptor)));

            final GenererTidsserieCommand genereringskommando = modus.createTidsserieCommand(overfoering, disruptor);
            registrer(GenererTidsserieCommand.class, genereringskommando);

            final LocalDateTime started = now();
            controller.validerGrunnlagsdata(grunnlagsdataValidator);
            controller.startBackend(backend);
            controller.lastOpp(overfoering);

            REGISTRY
                    .getServiceReferences(TidsserieLivssyklus.class)
                    .stream()
                    .map(REGISTRY::getService)
                    .flatMap(Optionals::stream)
                    .forEach(l -> l.start(REGISTRY));

            controller.lagTidsserie(
                    backend,
                    arguments.observasjonsperiode()
            );

            REGISTRY
                    .getServiceReferences(TidsserieLivssyklus.class)
                    .stream()
                    .map(REGISTRY::getService)
                    .flatMap(Optionals::stream)
                    .forEach(l -> l.stop(REGISTRY));

            controller.opprettMetadata(metaDataWriter, utKatalog, arguments, batchId, between(started, now()));

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

        shutdown(controller);
    }

    /**
     * Registrerer {@code tjeneste} i tjenesteregisteret under typen {@code type} med ein eller fleire valgfrie {@code
     * egenskapar}.
     *
     * @param <T> tjenestetypen
     * @param type tjenestetypen tjenesta skal registrerast under i tjenesteregisteret
     * @param tjeneste tjenesteinstansen som skal registrerast
     * @param egenskapar 0 eller fleire egenskapar på formatet egenskap=verdi
     * @return registreringa for tjenesta
     */
    private static <T> ServiceRegistration<T> registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        return REGISTRY.registerService(type, tjeneste, egenskapar);
    }

    private static DirectoryCleaner createDirectoryCleaner(int slettEldreEnn, Path logKatalog, Path dataKatalog) throws HousekeepingException {
        DeleteBatchDirectoryFinder finder = new DeleteBatchDirectoryFinder(dataKatalog, logKatalog);
        Path[] deleteDirectories = finder.findDeletableBatchDirectories(slettEldreEnn);
        return new DirectoryCleaner(deleteDirectories);
    }

    private static void shutdown(ApplicationController controller) {
        controller.logExit();
        System.exit(controller.exitCode());
    }

    private static void startBatchTimeout(ProgramArguments arguments, ApplicationController controller) {
        new BatchTimeoutTaskrunner(
                startBatchTimeout(arguments)).startTerminationTimeout
                (
                        ofMinutes(0),
                        () -> {
                            controller.logTimeout();
                            shutdown(controller);
                        }
                );
    }

    private static BatchTimeout startBatchTimeout(ProgramArguments arguments) {
        return new BatchTimeout(arguments.getKjoeretid(), arguments.getSluttidspunkt()).start();
    }

}
