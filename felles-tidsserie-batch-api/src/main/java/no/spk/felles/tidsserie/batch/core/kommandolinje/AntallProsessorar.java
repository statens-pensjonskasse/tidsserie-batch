package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.rangeClosed;

import java.util.function.Supplier;
import java.util.stream.IntStream;

import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;

/**
 * {@link AntallProsessorar} representerer antall prosessorar som {@link Tidsseriemodus} skal
 * benytte for prosesseringsformål.
 * <p>
 * Prosessorar som blir benytta for skriving av resultat tilbake til disk er utelatt frå denne verdien,
 * det pr i dag ikkje noko batchen lar brukaren overstyre antallet på.
 *
 * @since 1.1.0
 */
public class AntallProsessorar {
    private static Supplier<Integer> AVAILABLE_PROCESSORS;

    static {
        reset();
    }

    private final Integer verdi;

    private AntallProsessorar(final int verdi) {
        if (verdi < 1) {
            throw new IllegalArgumentException("parallellitet må vere eit positivt heiltall større enn 0, var " + verdi);
        }
        this.verdi = verdi;
    }

    /**
     * Opprettar ein ny instans med <code>antall</code> som antall prosessorar som batchen skal benytte
     * til prosesseringsformål.
     *
     * @param antall antall prosessorar batchen skal benytte til generering av tidsseriar
     * @return ein ny instans med det angitte antall prosessorar
     * @throws IllegalArgumentException dersom <code>antall</code> er mindre enn 1
     */
    public static AntallProsessorar antallProsessorar(final int antall) {
        return new AntallProsessorar(antall);
    }

    public static AntallProsessorar availableProcessors() {
        return antallProsessorar(AVAILABLE_PROCESSORS.get());
    }

    public static AntallProsessorar standardAntallProsessorar() {
        return antallProsessorar(
                Math.max(
                        availableProcessors().verdi - 1,
                        1
                )
        );
    }

    public static boolean erGyldig(final int antall) {
        return antall > 0 && antall <= availableProcessors().verdi;
    }

    /**
     * Genererer ein straum som inneheld alle tall frå og med 1 til og med antall prosessorar sin verdi.
     *
     * @return ein straum med alle tall frå 1 til antall prosessorar sin verdi
     */
    public IntStream stream() {
        return rangeClosed(1, verdi);
    }

    public int antall() {
        return verdi;
    }

    @Override
    public int hashCode() {
        return verdi.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AntallProsessorar that = (AntallProsessorar) obj;
        return this.verdi.equals(that.verdi);
    }

    @Override
    public String toString() {
        return "antall prosessorar " + verdi;
    }

    /**
     * Kun for testformål.
     *
     * @param availableProcessors ein stub som angir antall tilgjengelige CPUar på maskina
     */
    static void overstyr(final Supplier<Integer> availableProcessors) {
        AVAILABLE_PROCESSORS = requireNonNull(availableProcessors, "availableProcessors er påkrevd, men var null");
    }

    /**
     * Kun for testformål.
     */
    static void reset() {
        overstyr(Runtime.getRuntime()::availableProcessors);
    }
}