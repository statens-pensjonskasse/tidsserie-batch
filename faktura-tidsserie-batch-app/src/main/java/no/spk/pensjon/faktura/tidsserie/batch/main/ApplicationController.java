package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.upload.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import org.slf4j.Logger;
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
        System.setProperty("batchKatalog", utKatalog.toString());
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback/logback.xml");
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

    public void ryddOpp(DirectoryCleaner directoryCleaner) throws HousekeepingException {
        view.informerOmOppryddingStartet();
        directoryCleaner.deleteDirectories();
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
                return EXIT_SUCCESS;
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

    public void lagTidsserie(TidsserieBackendService backend, final Observasjonsperiode periode) {
        view.startarTidsseriegenerering(periode.fraOgMed(), periode.tilOgMed().get());
        Map<String, Integer> meldingar = backend.lagTidsserie();
        view.tidsseriegenereringFullfoert(meldingar);
    }

    public void opprettMetadata(MetaDataWriter metaDataWriter, ProgramArguments arguments, BatchId batchId, Duration duration) {
        view.informerOmMetadataOppretting();
        metaDataWriter.createMetadataFile(arguments, batchId, duration);
    }

    /**
     * Informerer bruker om at opprydding i kataloger feilet, og markerer batchen som feilet.
     */
    public void informerOmFeiletOpprydding() {
        view.informerOmFeiletOpprydding();
        markerSomFeilet();
    }

    /**
     * Logger at programmet har brukt for lang tid, og informerer bruker om dette. Marker batchen som feilet.
     */
    public void logTimeout() {
        view.informerOmTimeout();
        markerSomFeilet();
        getLogger().warn("Timeout - Batchen har brukt for lang tid på å kjøre, og vil bli avsluttet.");
    }

    /**
     * Logger exit-kode for tilstanden ApplicationController har nå. Denne metoden bør (skal) bare kalles når programmet avsluttes.
     */
    public void logExit(){
        getLogger().info("Exit code: " + exitCode());
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(ApplicationController.class);
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
