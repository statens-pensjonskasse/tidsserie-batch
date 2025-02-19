package no.spk.premie.tidsserie.batch.main;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static no.spk.premie.tidsserie.batch.core.registry.Ranking.ranking;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.ClassicConstants;
import no.spk.faktura.input.BatchId;
import no.spk.premie.tidsserie.batch.core.Tidsseriemodus;
import no.spk.premie.tidsserie.batch.core.grunnlagsdata.LastOppGrunnlagsdataKommando;
import no.spk.premie.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.premie.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;
import no.spk.premie.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.premie.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.premie.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.premie.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.premie.tidsserie.batch.core.registry.Plugin;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ApplicationController styrer all kommunikasjon med sluttbrukaren og held på
 * applikasjonstilstanden som styrer exit-koda som batchen returnerer til
 * prosessen som batchen blir køyrt frå.
 * <p>
 * NB: Kontrolleren kan ikkje bruke eller instansiere nokon form for logger eller logging før tidligast i
 * eller etter at {@link #informerOmOppstart(TidsserieBatchArgumenter)} har blitt kalla. Om så blir forsøkt
 * vil ikkje batchens loggfil inneholde meldinga.
 *
 * @author Snorre E. Brekke - computas
 */
public class ApplicationController {
    static final int EXIT_SUCCESS = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_WARNING = 2;

    private final Extensionpoint<UttrekksValidator> validator;
    private final Extensionpoint<LastOppGrunnlagsdataKommando> opplasting;
    private final Extensionpoint<MedlemsdataBackend> medlemsdata;
    private final Extensionpoint<Plugin> plugins;
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
        this.validator = new Extensionpoint<>(UttrekksValidator.class, registry);
        this.opplasting = new Extensionpoint<>(LastOppGrunnlagsdataKommando.class, registry);
        this.medlemsdata = new Extensionpoint<>(MedlemsdataBackend.class, registry);
        this.plugins = new Extensionpoint<>(Plugin.class, registry);
    }

    public void initialiserLogging(final BatchId id, final Path utKatalog) {
        System.setProperty("batchKatalog", utKatalog.toString());
        System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, "logback/logback.xml");
        MDC.put("batchId", id.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownLogger, "Batch shutdown"));

        logger = Optional.of(LoggerFactory.getLogger(ApplicationController.class));
        view = new ConsoleView(logger.get());
        registry.registerService(View.class, view, ranking(100).egenskap());
    }

    public void informerOmOppstart(final TidsserieBatchArgumenter argumenter) {
        view.informerOmOppstart(argumenter);
    }

    public void validerGrunnlagsdata() {
        view.informerOmGrunnlagsdataValidering();
        validator.invokeFirst(UttrekksValidator::validate)
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

    public void informerOmBruk(final BruksveiledningSkalVisesException e) {
        view.visHjelp(e);
        markerSomSuksess();
    }

    public void informerOmUgyldigeArgumenter(final UgyldigKommandolinjeArgumentException e) {
        view.informerOmUgyldigKommandolinjeArgument(e);
        markerSomFeilet();
    }

    public void informerOmUkjentFeil(final Exception e) {
        view.informerOmUkjentFeil();
        LoggerFactory.getLogger(ApplicationController.class).error("Exeption i main", e);
        markerSomFeilet();
    }

    public void informerOmKorrupteGrunnlagsdata(UgyldigUttrekkException e) {
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

    public void startBackend() {
        view.startarBackend();
        this.medlemsdata
                .invokeFirst(MedlemsdataBackend::start)
                .orElseRethrowFirstFailure();
    }

    public void lastOpp() {
        view.startarOpplasting();
        opplasting
                .invokeAll(kommando -> kommando.lastOpp(registry))
                .orElseRethrowFirstFailure()
        ;
        view.opplastingFullfoert();
    }

    public void lagTidsserie(ServiceRegistry registry, Tidsseriemodus modus) {
        view.startarTidsseriegenerering();
        Map<String, Integer> meldingar = modus.lagTidsserie(registry);
        view.tidsseriegenereringFullfoert(meldingar, modus.navn());
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


    /**
     * Notifiserer alle {@link Plugin#aktiver(ServiceRegistry) plugins} om at dei no kan registrere tjenester
     * i tjenesteregisteret.
     *
     * @since 1.1.0
     */
    public void aktiverPlugins() {
        plugins
                .invokeAll(plugin -> plugin.aktiver(registry))
                .orElseThrow(this::feilVedPluginAktivering);
    }

    private IllegalStateException feilVedPluginAktivering(final Stream<RuntimeException> alleFeil) {
        final List<RuntimeException> feil = alleFeil.collect(Collectors.toList());
        final IllegalStateException e = new IllegalStateException(
                format(
                        "Aktivering av %d plugins feila.\n\nFeilmeldingar frå aktiveringa:\n%s",
                        feil.size(),
                        feil
                                .stream()
                                .map(Throwable::getMessage)
                                .map(feilmelding -> "- " + feilmelding)
                                .collect(joining("\n"))
                )
        );
        feil.forEach(e::addSuppressed);
        return e;
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
