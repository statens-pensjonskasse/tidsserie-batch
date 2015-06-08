package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.io.File;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

/**
 * TODO: Kva og korleis ønskjer vi å vise status for batchkøyringa når vi køyrer den for vår egen bruk?
 */
public class View {
    public void startarBackend() {
    }

    public void startarOpplasting() {
    }

    public void opplastingFullfoert() {
    }

    public void startarTidsseriegenerering(File malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) {
    }

    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar) {
    }

    public void fatalFeil(Exception e) {
    }

    public void ryddarOppFilerFraaTidligereKjoeringer() {
    }

    public void verifisererInput() {
    }
}
