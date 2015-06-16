package no.spk.pensjon.faktura.tidsserie.storage.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.MetaDataWriter;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.InvalidParameterException;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.UsageRequestedException;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

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
    private final TidsserieBackendService backend;

    private final GrunnlagsdataService overfoering;

    Main(final TidsserieBackendService backend, final GrunnlagsdataService overfoering) {
        this.backend = backend;
        this.overfoering = overfoering;
    }

    public static void main(String[] args) throws IOException {
        final ApplicationController controller = new ApplicationController();

        try {
            ProgramArguments arguments = ProgramArgumentsFactory.create(args);

            final BatchId batchId = new BatchId(LocalDateTime.now());
            controller.initialiserLogging(batchId, arguments.getUtkatalog());
            controller.informerOmOppstart(arguments);

            new GrunnlagsdataDirectoryValidator(arguments.getGrunnlagsdataBatchKatalog()).validate();

            controller.informerOmOppryddingStartet();
            Oppryddingsstatus oppryddingsstatus = new BatchDirectoryCleaner(arguments.getUtkatalog(), batchId).deleteAllPreviousBatches();
            controller.informerOmOpprydding(oppryddingsstatus);

            final TidsserieBackendService backend = new HazelcastBackend();
            final GrunnlagsdataRepository input = new CSVInput(arguments.getInnkatalog().resolve(arguments.getGrunnlagsdataBatchId()));
            final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);
            final Configuration freemarkerConfiguration = createTemplatingConfiguration();

            Path batchKatalog = batchId.tilArbeidskatalog(arguments.getUtkatalog());
            Files.createDirectories(batchKatalog);

            final Main main = new Main(backend, overfoering);

            main.run(controller,
                    new FileTemplate(batchKatalog, "output-", ".csv"),
                    new Aarstall(arguments.getFraAar()),
                    new Aarstall(arguments.getTilAar()));

            MetaDataWriter metaDataWriter = new MetaDataWriter(freemarkerConfiguration, batchKatalog);
            metaDataWriter.createMetadataFile(arguments, batchId);
            metaDataWriter.createChecksumFile();

            controller.informerOmSuksess(batchKatalog);
        } catch (InvalidParameterException e) {
            controller.informerOmUgyldigeArgumenter(e);
        } catch (UsageRequestedException e) {
            controller.informerOmBruk(e);
        } catch (GrunnlagsdataException e) {
            controller.informerOmKorrupteGrunnlagsdata(e);
        }catch (IOException e){
            controller.informerOmUkjentFeil(e);
        } catch (final Exception e) {
            controller.informerOmUkjentFeil(e);
        }

        System.exit(controller.exitCode());
    }

    private void run(final ApplicationController controller, final FileTemplate malFilnavn, final Aarstall fraOgMed, final Aarstall tilOgMed) throws IOException {
        //TODO start timeout timer - refaktorere så vi kan kjøre en timeout-tråd og terminere hele programmet men samtidig logge/rydde opp?

        controller.startarBackend();
        backend.start();

        controller.startarOpplasting();
        overfoering.lastOpp();
        controller.opplastingFullfoert();

        controller.startarTidsseriegenerering(malFilnavn, fraOgMed, tilOgMed);
        Map<String, Integer> meldingar = backend.lagTidsseriePaaStillingsforholdNivaa(
                malFilnavn,
                fraOgMed,
                tilOgMed
        );

        controller.tidsseriegenereringFullfoert(meldingar);
    }

    private static Configuration createTemplatingConfiguration() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_22);
        config.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "templates");
        config.setDefaultEncoding("cp1252");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return config;
    }

}
