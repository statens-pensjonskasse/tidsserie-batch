package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.InvalidParameterException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;

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
public class Main {
    public static void main(String[] args) {
        final ApplicationController controller = new ApplicationController(new ConsoleView());

        try {
            ProgramArguments arguments = ProgramArgumentsFactory.create(args);
            final BatchId batchId = new BatchId(LocalDateTime.now());
            Path batchKatalog = batchId.tilArbeidskatalog(arguments.getUtkatalog());

            Files.createDirectories(batchKatalog);
            controller.initialiserLogging(batchId, arguments.getUtkatalog());
            controller.informerOmOppstart(arguments);

            GrunnlagsdataDirectoryValidator grunnlagsdataValidator = new GrunnlagsdataDirectoryValidator(arguments.getGrunnlagsdataBatchKatalog());
            controller.validerGrunnlagsdata(grunnlagsdataValidator);

            BatchDirectoryCleaner directoryCleaner = new BatchDirectoryCleaner(arguments.getUtkatalog(), batchId);
            controller.ryddOpp(directoryCleaner);

            final Tidsseriemodus parameter = arguments.modus();
            final TidsserieBackendService backend = new HazelcastBackend(arguments.getNodes(), parameter);
            final GrunnlagsdataRepository input = new CSVInput(arguments.getInnkatalog().resolve(arguments.getGrunnlagsdataBatchId()));
            final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);
            final Configuration freemarkerConfiguration = TemplateConfigurationFactory.create();

            long started = System.currentTimeMillis();
            controller.startBackend(backend);

            controller.lastOpp(overfoering);
            controller.lagTidsserie(backend,
                    new FileTemplate(batchKatalog, "output-", ".csv"),
                    new Aarstall(arguments.getFraAar()),
                    new Aarstall(arguments.getTilAar()));

            Duration duration = Duration.of(System.currentTimeMillis() - started, ChronoUnit.MILLIS);

            MetaDataWriter metaDataWriter = new MetaDataWriter(freemarkerConfiguration, batchKatalog);
            controller.opprettMetadata(metaDataWriter, arguments, batchId, duration);

            controller.informerOmSuksess(batchKatalog);
        } catch (InvalidParameterException e) {
            controller.informerOmUgyldigeArgumenter(e);
        } catch (UsageRequestedException e) {
            controller.informerOmBruk(e);
        } catch (GrunnlagsdataException e) {
            controller.informerOmKorrupteGrunnlagsdata(e);
        } catch (final Exception e) {
            controller.informerOmUkjentFeil(e);
        }

        System.exit(controller.exitCode());
    }
}
