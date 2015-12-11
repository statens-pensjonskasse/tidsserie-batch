package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.Duration.of;
import static java.time.Duration.ofMinutes;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.TIDSSERIE_PREFIX;
import static no.spk.pensjon.faktura.tidsserie.core.TidsserieResulat.tidsserieResulat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.faktura.timeout.BatchTimeout;
import no.spk.faktura.timeout.BatchTimeoutTaskrunner;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.TidsserieArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.upload.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;

import freemarker.template.Configuration;

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
    public static void main(String[] args) {
        final ApplicationController controller = new ApplicationController(new ConsoleView());

        try {
            ProgramArguments arguments = new TidsserieArgumentsFactory().create(args);
            startBatchTimeout(arguments, controller);

            final BatchId batchId = new BatchId(TIDSSERIE_PREFIX, LocalDateTime.now());
            Path batchLogKatalog = batchId.tilArbeidskatalog(arguments.getLogkatalog());
            Path dataKatalog = arguments.getUtkatalog().resolve("tidsserie");

            Files.createDirectories(batchLogKatalog);
            controller.initialiserLogging(batchId, batchLogKatalog);
            controller.informerOmOppstart(arguments);

            GrunnlagsdataDirectoryValidator grunnlagsdataValidator = new GrunnlagsdataDirectoryValidator(arguments.getGrunnlagsdataBatchKatalog());
            controller.validerGrunnlagsdata(grunnlagsdataValidator);

            DirectoryCleaner directoryCleaner = createDirectoryCleaner(arguments.getSlettLogEldreEnn(), arguments.getLogkatalog(), dataKatalog);
            controller.ryddOpp(directoryCleaner);

            Files.createDirectories(dataKatalog);

            final Tidsseriemodus modus = arguments.modus();
            final TidsserieBackendService backend = new HazelcastBackend(arguments.getNodes(), modus);
            final GrunnlagsdataRepository input = modus.repository(arguments.getInnkatalog().resolve(arguments.getGrunnlagsdataBatchId()));
            final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);
            final Configuration freemarkerConfiguration = TemplateConfigurationFactory.create();

            long started = System.currentTimeMillis();
            controller.startBackend(backend);

            controller.lastOpp(overfoering);
            controller.lagTidsserie(backend,
                    new FileTemplate(dataKatalog, "tidsserie", ".csv"),
                    new Aarstall(arguments.getFraAar()),
                    new Aarstall(arguments.getTilAar()));

            modus.completed(tidsserieResulat(dataKatalog).bygg());

            Duration duration = of(System.currentTimeMillis() - started, ChronoUnit.MILLIS);

            MetaDataWriter metaDataWriter = new MetaDataWriter(freemarkerConfiguration, batchLogKatalog);
            controller.opprettMetadata(metaDataWriter, dataKatalog, arguments, batchId, duration);

            controller.informerOmSuksess(batchLogKatalog);
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
