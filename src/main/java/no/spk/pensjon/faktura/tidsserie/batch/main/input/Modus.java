package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static java.util.Arrays.asList;

import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.batch.storage.csv.prognoseobservasjonar.Stillingsforholdprognosemodus;
import no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder.LiveTidsseriemodus;

/**
 * {@link Modus} representerer dei forskjellige modusane som brukaren
 * kan velge mellom for å styre output-formatet på CSV-filene batchen genererer.
 */
enum Modus {
    LIVE_TIDSSERIE("live_tidsserie", new LiveTidsseriemodus()),
    STILLINGSFORHOLD_OBSERVASJONAR("stillingsforholdobservasjonar", new Stillingsforholdprognosemodus());

    private final String kode;

    private final Tidsseriemodus modus;

    Modus(final String kode, final Tidsseriemodus modus) {
        this.kode = kode;
        this.modus = modus;
    }

    /**
     * Slår opp modusen som brukar den angitte koda.
     *
     * @param kode tekstlig-kode som skal konverterast til ein modus
     * @return modusen som brukar den angitte koda, eller ingenting dersom koda ikkje matchar ein kjent modus
     */
    static Optional<Modus> parse(final String kode) {
        return stream()
                .filter(v -> v.kode.equals(kode))
                .findFirst();
    }

    static Stream<Modus> stream() {
        return asList(values()).stream();
    }

    String kode() {
        return kode;
    }

    Tidsseriemodus modus() {
        return modus;
    }

    @Override
    public String toString() {
        return kode;
    }
}
