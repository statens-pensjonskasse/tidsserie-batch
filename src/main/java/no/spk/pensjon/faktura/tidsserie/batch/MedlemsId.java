package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;
import static no.spk.pensjon.faktura.tidsserie.batch.Validators.require;

/**
 * {@link MedlemsId} blir brukt for � unikt identifisere medlemmet eit sett med medlemsdata tilh�yrer.
 * <br>
 * F�dselsnummer er bygd opp som ei 13-sifra kode som best�r av to delar:
 * <ol>
 * <li>8-sifra f�dselsdato p� format yyyyMMdd</li>
 * <li>5-sifra personnummer</li>
 * </ol>
 * <br>
 * Grunna bruken av tall som representasjonsform for b�de f�dselsdato og personnummer i kasper, vil personnummer med
 * verdi under 10000, inneholde f�rre enn 5-siffer. For � normalisere desse bli dei derfor padda med p�krevd
 * antall 0-siffer i front av personnummeret slik at det alltid blir behandla vidare som ei 5-sifra kode.
 *
 * @author Tarjei Skorgenes
 */
public class MedlemsId {
    public static final int PERSONNUMMER_LENGDE = 5;
    private final String text;

    /**
     * Konstruerer eit nytt f�dselsnummer.
     * <br>
     * Verdiane blir syntaktisk validert, det blir ikkje validert korvidt f�dseldatoen er ein gyldig dato
     * eller korvidt personnummeret er eit gyldig personnummer.
     * <br>
     * Personnummer blir automatisk utvida til 5-siffer viss det inneheld eit 1- til 4-sifra tall. Utvidelsen
     * skjer ved � legge til ledande 0ara slik at den endelige koda blir 5-sifra.
     *
     * @param foedselsdato f�dselsdato, kun eit 8-sifra tall blir godtatt
     * @param personnummer personnummeret, 1-5 siffer blir godtatt
     * @throws NullPointerException     dersom nokon av argumenta er <code>null</code>
     * @throws IllegalArgumentException dersom f�dselsdato ikkje er eit 8-sifra tall eller dersom personnummer ikkje
     *                                  er eit 1- til 5-sifra tall
     */
    public MedlemsId(final String foedselsdato, final String personnummer) {
        final String korrigertFoedselsdato = require(
                requireNonNull(foedselsdato, "F�dselsdato er p�krevd, men var null")
                        .trim(),
                value -> value.matches("[0-9]{8}"),
                value -> new IllegalArgumentException("F�dselsdato m� vere eit 8-sifra tall, var " + value)
        );
        final String korrigertPersonnummer = padWithLeadingZeroes(
                require(
                        requireNonNull(personnummer, "Personnummer er p�krevd, men var null")
                                .trim(),
                        value -> value.length() <= PERSONNUMMER_LENGDE,
                        value -> new IllegalArgumentException("Personnummer m� vere eit 5-sifra tall, var " + value)
                )
        );
        require(
                korrigertPersonnummer,
                value -> value.matches("[0-9]{5}"),
                value -> new IllegalArgumentException("Personnummer m� vere eit 5-sifra tall, var " + value)
        );
        this.text = korrigertFoedselsdato + korrigertPersonnummer;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == getClass()) {
            final MedlemsId other = (MedlemsId) obj;
            return text.equals(other.text);
        }
        return false;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Konstruerer eit nytt f�dselsnummer.
     *
     * @param foedselsdato f�dselsdato, kun eit 8-sifra tall blir godtatt
     * @param personnummer personnummeret, 1-5 siffer blir godtatt
     * @return det nye f�dselsnummeret
     * @see #MedlemsId(String, String)
     */
    public static MedlemsId medlemsId(final String foedselsdato, String personnummer) {
        return new MedlemsId(foedselsdato, personnummer);
    }

    private String padWithLeadingZeroes(final String input) {
        final StringBuilder builder = new StringBuilder();
        final int count = PERSONNUMMER_LENGDE - input.length();
        for (int i = 0; i < count; i++) {
            builder.append('0');
        }
        builder.append(input);
        return builder.toString();
    }
}
