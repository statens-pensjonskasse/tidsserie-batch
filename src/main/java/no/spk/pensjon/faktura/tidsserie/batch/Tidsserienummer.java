package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;
import static no.spk.pensjon.faktura.tidsserie.batch.Validators.require;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * {@link Tidsserienummer} er ein semi-unik identifikator som lar ein skille to tidsseriar fr� kvarandre.
 * <br>
 * Intensjonen med identifikatoren er � kunne skille to tidsseriar generert p� forskjellige datoar fr� kvarandre.
 * Forel�pig antas det at ein ikkje kjem til � ha behov for � generere meir enn 1 tidsserie pr dag, ergo
 * blir dagens dato brukt som identifikator. Ein vil dermed ikkje vere i stand til � skille to tidsseriar generert
 * samme dag fr� kvarandre basert p� denne identifikatoren.
 * <br>
 * For � ta h�gde for at ein i framtida kan f� behov for � generere fleire tidsseriar pr dag, er det satt av
 * opp til 9 siffer i CSV-formatet mot DVH slik at ein kan legge til eit ekstra siffer som lar ein generere
 * opp til 9 tidsseriar pr dag f�r ein f�r samme problem p� nytt.
 *
 * @author Tarjei Skorgenes
 * @since 1.0.2
 */
public final class Tidsserienummer {
    private static DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String id;

    private Tidsserienummer(final String id) {
        this.id = require(
                requireNonNull(id, "tidsserienummer er p�krevd, men var null"),
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
     * Genererer eit nytt tidsserienummer basert p� den angitte datoen.
     * <br>
     * Tidsserienummeret blir generert basert p� datoen i yyyyMMdd-format.
     *
     * @param dato datoen tidsserienummeret skal benytte
     * @return eit nytt tidsserienummer basert p� angitt dato
     * @throws java.lang.NullPointerException     viss <code>dato</code> er <code>null</code>
     * @throws java.lang.IllegalArgumentException viss <code>dato</code> p� yyyyMMdd-format blir meir enn 8-siffer langt
     */
    public static Tidsserienummer genererForDato(final LocalDate dato) {
        return new Tidsserienummer(yyyyMMdd.format(dato));
    }

    private static RuntimeException feilLengdePaaId(final String id) {
        return new IllegalArgumentException(
                "tidsserienummer m� vere 8-siffer langt, men var "
                        + id.length() + "-siffer langt (" + id + ")"
        );
    }
}
