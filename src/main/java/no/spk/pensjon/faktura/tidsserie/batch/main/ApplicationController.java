package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.main.BatchId;
import no.spk.pensjon.faktura.tidsserie.storage.main.GrunnlagsdataException;
import no.spk.pensjon.faktura.tidsserie.storage.main.Main;
import no.spk.pensjon.faktura.tidsserie.storage.main.Oppryddingsstatus;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.InvalidParameterException;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.UsageRequestedException;

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
 * @author Tarjei Skorgenes
 */
public class ApplicationController {
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_ERROR = 1;
    private static final int EXIT_WARNING = 2;

    private Optional<Oppryddingsstatus> opprydding = Optional.empty();

    private int exitCode = EXIT_ERROR;

    /**
     * Kommuniserer med sluttbrukaren via kommandolinjas konsoll.
     *
     * @see System#out
     */
    private final View view = new ConsoleView();

    public void initialiserLogging(final BatchId id, final Path utKatalog) {
        System.setProperty("batchKatalog", id.tilArbeidskatalog(utKatalog).toString());
        MDC.put("batchId", id.toString());
    }

    public void informerOmOppstart(final ProgramArguments argumenter) {
        view.informerOmOppstart(argumenter);
    }

    public void informerOmOppryddingStartet() {
        view.informerOmOppryddingStartet();
    }

    public void informerOmOpprydding(final Oppryddingsstatus status) {
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
        e.printStackTrace();
        LoggerFactory.getLogger(Main.class).error("Exeption i main", e);
        markerSomFeilet();
    }

    public void informerOmKorrupteGrunnlagsdata(GrunnlagsdataException e) {
        view.informerOmKorrupteGrunnlagsdata(e);
        markerSomFeilet();
    }

    public int exitCode() {
        switch (exitCode) {
            case EXIT_SUCCESS:
                return !opprydding().isSuccessful() ? EXIT_WARNING : EXIT_SUCCESS;
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

    private Oppryddingsstatus opprydding() {
        return opprydding
                .orElseThrow(() -> new IllegalStateException("Programmeringsfeil, opprydding av gamle kjøringer mangler"));
    }

    public void startarBackend() {
        view.startarBackend();
    }

    public void startarOpplasting() {
        view.startarOpplasting();
    }

    public void opplastingFullfoert() {
        view.opplastingFullfoert();
    }

    public void startarTidsseriegenerering(FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) {
        view.startarTidsseriegenerering(malFilnavn, fraOgMed, tilOgMed);
    }

    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar) {
        view.tidsseriegenereringFullfoert(meldingar);
    }
}
