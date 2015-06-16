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
import no.spk.pensjon.faktura.tidsserie.batch.main.View;
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

    private int exitCode = 0;

    Main(final TidsserieBackendService backend, final GrunnlagsdataService overfoering) {
        this.backend = backend;
        this.overfoering = overfoering;
    }

    public static void main(String[] args) throws IOException {
        final View view = new View();

        try {
            ProgramArguments programArguments = ProgramArgumentsFactory.create(args);
            view.informerOmOppstart(programArguments);

            final TidsserieBackendService backend = new HazelcastBackend();
            final GrunnlagsdataRepository input = new CSVInput(programArguments.getInnkatalog().resolve(programArguments.getGrunnlagsdataBatchId()));
            final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);
            final Configuration freemarkerConfiguration = createTemplatingConfiguration();

            final BatchId batchId = new BatchId(LocalDateTime.now());

            view.ryddarOppFilerFraaTidligereKjoeringer();
            new BatchDirectoryCleaner(programArguments.getUtkatalog()).deleteAllPreviousBatches();

            view.verifisererInput();
            new GrunnlagsdataDirectoryValidator(programArguments.getGrunnlagsdataBatchKatalog()).validate();

            Path batchKatalog = batchId.tilArbeidskatalog(programArguments.getUtkatalog());
            Files.createDirectories(batchKatalog);

            final Main main = new Main(backend, overfoering);

            main.run(view,
                    new FileTemplate(batchKatalog, "output-", ".csv"),
                    new Aarstall(programArguments.getFraAar()),
                    new Aarstall(programArguments.getTilAar()));

            MetaDataWriter metaDataWriter = new MetaDataWriter(freemarkerConfiguration, batchKatalog);
            metaDataWriter.createMetadataFile(programArguments, batchId);
            metaDataWriter.createChecksumFile();

            System.exit(main.exitCode);
        } catch (InvalidParameterException e) {
            view.informerOmUgyldigKommandolinjeArgument(e);
        } catch (UsageRequestedException e) {
            view.visHjelp(e);
        }
        System.exit(1);
    }

    private void run(final View view, final FileTemplate malFilnavn, final Aarstall fraOgMed, final Aarstall tilOgMed) {
        try {

            //TODO start timeout timer - refaktorere så vi kan kjøre en timeout-tråd og terminere hele programmet men samtidig logge/rydde opp?

            view.startarBackend();
            backend.start();

            view.startarOpplasting();
            overfoering.lastOpp();
            view.opplastingFullfoert();

            view.startarTidsseriegenerering(malFilnavn, fraOgMed, tilOgMed);
            Map<String, Integer> meldingar = backend.lagTidsseriePaaStillingsforholdNivaa(
                    malFilnavn,
                    fraOgMed,
                    tilOgMed
            );

            view.tidsseriegenereringFullfoert(meldingar);
        } catch (final Exception e) {
            view.fatalFeil(e);
            exitCode = 1;
        }
    }

    private static Configuration createTemplatingConfiguration() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_22);
        config.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "templates");
        config.setDefaultEncoding("cp1252");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return config;
    }

}
