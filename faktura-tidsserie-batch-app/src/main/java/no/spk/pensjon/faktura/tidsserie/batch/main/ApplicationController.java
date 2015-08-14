package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.InvalidParameterException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ApplicationController styrer all kommunikasjon med sluttbrukaren og held på
 * applikasjonstilstanden som styrer exit-koda som batchen returnerer til
 * prosessen som batchen blir køyrt frå.
 * <p>
 * NB: Kontrolleren kan ikkje bruke eller instansiere nokon form for logger eller logging før tidligast i
 * eller etter at {@link #informerOmOppstart(ProgramArguments)} har blitt kalla. Om så blir forsøkt
 * vil ikkje batchens loggfil inneholde meldinga.
 *
 * @author Snorre E. Brekke - computas
 */
public class ApplicationController {
    static final int EXIT_SUCCESS = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_WARNING = 2;

    private Optional<Oppryddingsstatus> opprydding = Optional.empty();

    private int exitCode = EXIT_ERROR;

    /**
     * Kommuniserer med sluttbrukaren via kommandolinjas konsoll.
     *
     * @see System#out
     */
    private final View view;

    public ApplicationController(View view) {
        this.view = view;
    }

    public void initialiserLogging(final BatchId id, final Path utKatalog) {
        System.setProperty("batchKatalog", id.tilArbeidskatalog(utKatalog).toString());
        MDC.put("batchId", id.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownLogger, "Batch shutdown"));
    }

    public void informerOmOppstart(final ProgramArguments argumenter) {
        view.informerOmOppstart(argumenter);
    }

    public void validerGrunnlagsdata(GrunnlagsdataDirectoryValidator validator){
        view.informerOmGrunnlagsdataValidering();
        validator.validate();
    }

    public void ryddOpp(BatchDirectoryCleaner directoryCleaner) {
        view.informerOmOppryddingStartet();
        Oppryddingsstatus oppryddingsstatus = directoryCleaner.deleteAllPreviousBatches();
        informerOmOpprydding(oppryddingsstatus);
    }

    private void informerOmOpprydding(final Oppryddingsstatus status) {
        this.opprydding = of(requireNonNull(status));
        if (!status.isSuccessful()) {
            view.informerOmUslettbareArbeidskatalogar(status);
        }
    }

    public void informerOmSuksess(final Path arbeidskatalog) {
        view.informerOmSuksess(arbeidskatalog);
        markerSomSuksess();
    }

    public void informerOmBruk(final UsageRequestedException e) {
        view.visHjelp(e);
        markerSomSuksess();
    }

    public void informerOmUgyldigeArgumenter(final InvalidParameterException e) {
        view.informerOmUgyldigKommandolinjeArgument(e);
        markerSomFeilet();
    }

    public void informerOmUkjentFeil(final Exception e) {
        view.informerOmUkjentFeil();
        LoggerFactory.getLogger(ApplicationController.class).error("Exeption i main", e);
        markerSomFeilet();
    }

    public void informerOmKorrupteGrunnlagsdata(GrunnlagsdataException e) {
        view.informerOmKorrupteGrunnlagsdata(e);
        markerSomFeilet();
    }

    public int exitCode() {
        switch (exitCode) {
            case EXIT_SUCCESS:
                return !opprydding.orElseGet(Oppryddingsstatus::new).isSuccessful() ? EXIT_WARNING : EXIT_SUCCESS;
            case EXIT_WARNING:
            case EXIT_ERROR:
                return exitCode;
            default:
                return EXIT_ERROR;
        }
    }

    private void markerSomSuksess() {
        exitCode = EXIT_SUCCESS;
    }

    private void markerSomFeilet() {
        exitCode = EXIT_ERROR;
    }

    public void startBackend(TidsserieBackendService backend) {
        view.startarBackend();
        backend.start();
    }

    public void lastOpp(GrunnlagsdataService overfoering) throws IOException{
        view.startarOpplasting();
        overfoering.lastOpp();
        view.opplastingFullfoert();
    }

    public void lagTidsserie(TidsserieBackendService backend, FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) {
        view.startarTidsseriegenerering(malFilnavn, fraOgMed, tilOgMed);;
        Map<String, Integer> meldingar = backend.lagTidsseriePaaStillingsforholdNivaa(
                malFilnavn,
                fraOgMed,
                tilOgMed
        );
        view.tidsseriegenereringFullfoert(meldingar);
    }

    public void opprettMetadata(MetaDataWriter metaDataWriter, ProgramArguments arguments, BatchId batchId, Duration duration) {
        view.informerOmMetadataOppretting();
        metaDataWriter.createMetadataFile(arguments, batchId, duration);
        metaDataWriter.createChecksumFile();
    }

    private void shutdownLogger() {
        try {
            //Delay shutdown to allow flushing.
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
