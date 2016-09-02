package no.spk.pensjon.faktura.tidsserie.batch.main;

import static no.spk.pensjon.faktura.tjenesteregister.Constants.SERVICE_RANKING;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.core.Extensionpoint;
import no.spk.pensjon.faktura.tidsserie.batch.core.LastOppGrunnlagsdataKommando;
import no.spk.pensjon.faktura.tidsserie.batch.core.ServiceLocator;
import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

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

    private final Extensionpoint<GrunnlagsdataDirectoryValidator> validator;
    private final Extensionpoint<LastOppGrunnlagsdataKommando> opplasting;
    private final ServiceRegistry registry;

    private Optional<Logger> logger = Optional.empty();
    private int exitCode = EXIT_ERROR;

    /**
     * Kommuniserer med sluttbrukaren via kommandolinjas konsoll.
     *
     * @see System#out
     */
    private View view;

    public ApplicationController(final ServiceRegistry registry) {
        this.registry = registry;
        this.view = new ServiceLocator(registry).firstMandatory(View.class);
        this.validator = new Extensionpoint<>(GrunnlagsdataDirectoryValidator.class, registry);
        this.opplasting = new Extensionpoint<>(LastOppGrunnlagsdataKommando.class, registry);
    }

    public void initialiserLogging(final BatchId id, final Path utKatalog) {
        System.setProperty("batchKatalog", utKatalog.toString());
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback/logback.xml");
        MDC.put("batchId", id.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownLogger, "Batch shutdown"));

        logger = Optional.of(LoggerFactory.getLogger(ApplicationController.class));
        view = new ConsoleView(logger.get());
        registry.registerService(View.class, view, SERVICE_RANKING + "=100");
    }

    public void informerOmOppstart(final ProgramArguments argumenter) {
        view.informerOmOppstart(argumenter);
    }

    public void validerGrunnlagsdata() {
        view.informerOmGrunnlagsdataValidering();
        validator.invokeFirst(GrunnlagsdataDirectoryValidator::validate)
                .orElseRethrowFirstFailure();
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

    public void startBackend(MedlemsdataBackend backend) {
        view.startarBackend();
        backend.start();
    }

    public void lastOpp() {
        view.startarOpplasting();
        opplasting
                .invokeFirst(kommando -> kommando.lastOpp(registry))
                .orElseRethrowFirstFailure()
        ;
        view.opplastingFullfoert();
    }

    public void lagTidsserie(ServiceRegistry registry, Tidsseriemodus modus, final Observasjonsperiode periode) {
        view.startarTidsseriegenerering(periode.fraOgMed(), periode.tilOgMed().get());
        Map<String, Integer> meldingar = modus.lagTidsserie(registry);
        view.tidsseriegenereringFullfoert(meldingar, modus.navn());
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
     * Logger exit-kode for tilstanden ApplicationController har nå. Denne metoden bør (skal) bare kalles når programmet
     * avsluttes.
     */
    public void logExit() {
        logger.ifPresent(l -> l.info("Exit code: " + exitCode()));
    }

    private Logger getLogger() {
        return logger.orElseThrow(() -> new IllegalStateException("Logger er ikke initialisert. Er #initialiserLogging blitt kjørt?"));
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
