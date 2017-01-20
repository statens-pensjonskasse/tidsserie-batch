package no.spk.felles.tidsserie.batch.main.input;

import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;

/**
 * {@link Modus} representerer dei forskjellige modusane som brukaren
 * kan velge mellom for å styre output-formatet på CSV-filene batchen genererer.
 */
public class Modus {
    private static final Set<Modus> SUPPORTED = new HashSet<>();

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
    public static Optional<Modus> parse(final String kode) {
        return stream()
                .filter(v -> v.kode.equals(kode))
                .findFirst();
    }

    /**
     * Returnerer alle modusar som har blitt plugga inn i batchen via {@link #reload(Stream)}.
     *
     * @return alle modusar batchen støttar
     */
    public static Stream<Modus> stream() {
        return SUPPORTED.stream();
    }

    public String kode() {
        return kode;
    }

    Tidsseriemodus modus() {
        return modus;
    }

    @Override
    public String toString() {
        return kode;
    }

    /**
     * Lastar inn lista med modusar som er plugga inn i batchen og som brukaren skal kunne velge mellom.
     *
     * @param modusar ei liste med alle tidsseriemodusar som er plugga inn i batchen
     */
    public static void reload(final Stream<Tidsseriemodus> modusar) {
        SUPPORTED.clear();
        modusar.map(m -> new Modus(m.navn(), m)).forEach(SUPPORTED::add);
    }

    /**
     * Autodetekterer alle modusar som er plugga inn i batchen via {@link ServiceLoader#load(Class)}.
     *
     * @see Tidsseriemodus
     */
    public static void autodetect() {
        reload(
                StreamSupport
                        .stream(
                                ServiceLoader
                                        .load(Tidsseriemodus.class)
                                        .spliterator(),
                                false
                        )
        );
    }
}
