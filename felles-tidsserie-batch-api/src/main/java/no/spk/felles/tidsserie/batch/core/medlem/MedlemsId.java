package no.spk.felles.tidsserie.batch.core.medlem;

import static java.util.Objects.requireNonNull;
import static no.spk.felles.tidsserie.batch.core.Validators.require;

/**
 * {@link MedlemsId} blir brukt for å unikt identifisere medlemmet eit sett med medlemsdata tilhøyrer.
 * <br>
 * Vi har ingen føringer på korleis medlemsid er formatert. Einaste kravet er ein ikkje-tom streng.
 *
 * @author Tarjei Skorgenes
 */
@SuppressWarnings("deprecation")
public class MedlemsId {
    public static final int PERSONNUMMER_LENGDE = 5;
    private final String text;

    /**
     * Deprecated. Bruk {@link #MedlemsId(String)}
     *
     * @param foedselsdato fødselsdato, kun eit 8-sifra tall blir godtatt
     * @param personnummer personnummeret, 1-5 siffer blir godtatt
     * @throws NullPointerException dersom nokon av argumenta er <code>null</code>
     * @throws IllegalArgumentException dersom fødselsdato ikkje er eit 8-sifra tall eller dersom personnummer ikkje
     * er eit 1- til 5-sifra tall
     * @see #MedlemsId(String)
     */
    @Deprecated
    public MedlemsId(final String foedselsdato, final String personnummer) {
        final String korrigertFoedselsdato = require(
                requireNonNull(foedselsdato, "Fødselsdato er påkrevd, men var null")
                        .trim(),
                value -> value.matches("[0-9]{8}"),
                value -> new IllegalArgumentException("Fødselsdato må vere eit 8-sifra tall, var " + value)
        );
        final String korrigertPersonnummer = padWithLeadingZeroes(
                require(
                        requireNonNull(personnummer, "Personnummer er påkrevd, men var null")
                                .trim(),
                        value -> value.length() <= PERSONNUMMER_LENGDE,
                        value -> new IllegalArgumentException("Personnummer må vere eit 5-sifra tall, var " + value)
                )
        );
        require(
                korrigertPersonnummer,
                value -> value.matches("[0-9]{5}"),
                value -> new IllegalArgumentException("Personnummer må vere eit 5-sifra tall, var " + value)
        );
        this.text = korrigertFoedselsdato + korrigertPersonnummer;
    }

    /**
     * Konstruerer ein ny medlemsidentifikator.
     * <br>
     * Verdien kan ikkje være tom eller berre innehalde whitespace
     *
     * @param id ein tekst som identifiserer eit bestemt medlem unikt
     * @throws NullPointerException dersom argumentet er <code>null</code>
     * @throws IllegalArgumentException dersom id-feltet er tomt
     * @since 1.1.0
     */
    public MedlemsId(String id) {
        this.text = require(
                requireNonNull(id, "id er påkrevd, men var null").trim(),
                value -> !value.isEmpty(),
                value -> new IllegalArgumentException("id er påkrevd, men var tom")
        );
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
     * Deprecated bruk {@link #medlemsId(String)}
     *
     * @param foedselsdato fødselsdato, kun eit 8-sifra tall blir godtatt
     * @param personnummer personnummeret, 1-5 siffer blir godtatt
     * @return det nye fødselsnummeret
     * @see #medlemsId(String)
     */
    @Deprecated
    public static MedlemsId medlemsId(final String foedselsdato, String personnummer) {
        return new MedlemsId(foedselsdato, personnummer);
    }

    /**
     * Konstruerer ein ny medlemsidentifikator
     *
     * @param id ein tekst som identifiserer eit bestemt medlem unikt
     * @return den nye medlemsidentifikatoren
     * @see #MedlemsId(String)
     * @since 1.1.0
     */
    public static MedlemsId medlemsId(final String id) {
        return new MedlemsId(id);
    }

    @Deprecated
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
