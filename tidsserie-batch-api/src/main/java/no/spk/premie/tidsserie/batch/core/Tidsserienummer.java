package no.spk.premie.tidsserie.batch.core;

import static java.util.Objects.requireNonNull;
import static no.spk.premie.tidsserie.batch.core.Validators.require;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * {@link Tidsserienummer} er ein semi-unik identifikator som lar ein skille to tidsseriar frå kvarandre.
 * <br>
 * Intensjonen med identifikatoren er å kunne skille to tidsseriar generert på forskjellige datoar frå kvarandre.
 * Foreløpig antas det at ein ikkje kjem til å ha behov for å generere meir enn 1 tidsserie pr dag, ergo
 * blir dagens dato brukt som identifikator. Ein vil dermed ikkje vere i stand til å skille to tidsseriar generert
 * samme dag frå kvarandre basert på denne identifikatoren.
 * <br>
 * For å ta høgde for at ein i framtida kan få behov for å generere fleire tidsseriar pr dag, er det satt av
 * opp til 9 siffer i CSV-formatet mot DVH slik at ein kan legge til eit ekstra siffer som lar ein generere
 * opp til 9 tidsseriar pr dag før ein får samme problem på nytt.
 *
 * @author Tarjei Skorgenes
 */
public final class Tidsserienummer {
    private static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String id;

    private Tidsserienummer(final String id) {
        this.id = require(
                requireNonNull(id, "tidsserienummer er påkrevd, men var null"),
                t -> t.matches("[0-9]{8}"),
                Tidsserienummer::feilLengdePaaId
        );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof  Tidsserienummer) {
            final Tidsserienummer other = (Tidsserienummer) obj;
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Genererer eit nytt tidsserienummer basert på den angitte datoen.
     * <br>
     * Tidsserienummeret blir generert basert på datoen i yyyyMMdd-format.
     *
     * @param dato datoen tidsserienummeret skal benytte
     * @return eit nytt tidsserienummer basert på angitt dato
     * @throws java.lang.NullPointerException     viss <code>dato</code> er <code>null</code>
     * @throws java.lang.IllegalArgumentException viss <code>dato</code> på yyyyMMdd-format blir meir enn 8-siffer langt
     */
    public static Tidsserienummer genererForDato(final LocalDate dato) {
        return new Tidsserienummer(yyyyMMdd.format(dato));
    }

    private static RuntimeException feilLengdePaaId(final String id) {
        return new IllegalArgumentException(
                "tidsserienummer må vere 8-siffer langt, men var "
                        + id.length() + "-siffer langt (" + id + ")"
        );
    }
}
