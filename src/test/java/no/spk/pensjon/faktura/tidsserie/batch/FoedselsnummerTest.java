package no.spk.pensjon.faktura.tidsserie.batch;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class FoedselsnummerTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test
    public void skalVerifisereFoedselsdatoSyntaktisk() {
        final String description = "F�dselsdato m� vere eit 8-sifra tall";

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979010101",
                "12345"
        )).as("feil n�r f�dselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "    1979010101    ",
                "12345"
        )).as("feil n�r f�dselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979",
                "12345"
        )).as("feil n�r f�dselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979    ",
                "12345"
        )).as("feil n�r f�dselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979ABCD",
                "12345"
        )).as("feil n�r f�dselsdato ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979ABCD");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "  ABCD  ",
                "12345"
        )).as("feil n�r f�dselsdato ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABCD");
    }

    @Test
    public void skalVerifiserePersonnummerSyntaktisk() {
        final String description = "Personnummer m� vere eit 5-sifra tall";
        final String dato = "19790101";

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                "123456"
        )).as("feil n�r personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " 123456 "
        )).as("feil n�r personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                "ABCDEF"
        )).as("feil n�r personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABCDEF");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " ABC "
        )).as("feil n�r personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 00ABC");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " ABC00 "
        )).as("feil n�r personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABC00");
    }

    @Test
    public void skalEkspanderePersonnummerTil5SifferAutomatisk() {
        final String dato = "19790706";

        softly.assertThat(new Foedselsnummer(dato, "1")).isEqualTo(new Foedselsnummer(dato, "00001"));
        softly.assertThat(new Foedselsnummer(dato, "10")).isEqualTo(new Foedselsnummer(dato, "00010"));
        softly.assertThat(new Foedselsnummer(dato, "100")).isEqualTo(new Foedselsnummer(dato, "00100"));
        softly.assertThat(new Foedselsnummer(dato, "1000")).isEqualTo(new Foedselsnummer(dato, "01000"));
        softly.assertThat(new Foedselsnummer(dato, "10000")).isEqualTo(new Foedselsnummer(dato, "10000"));

        softly.assertThat(new Foedselsnummer(dato, "    1")).isEqualTo(new Foedselsnummer(dato, "00001"));
        softly.assertThat(new Foedselsnummer(dato, "   10")).isEqualTo(new Foedselsnummer(dato, "00010"));
        softly.assertThat(new Foedselsnummer(dato, "  100")).isEqualTo(new Foedselsnummer(dato, "00100"));
        softly.assertThat(new Foedselsnummer(dato, " 1000")).isEqualTo(new Foedselsnummer(dato, "01000"));
        softly.assertThat(new Foedselsnummer(dato, "10000")).isEqualTo(new Foedselsnummer(dato, "10000"));
    }

    /**
     * For � vere konsistent med handteringa av personnummer, verifiser at ogs� f�dselsdato blir trimma for
     * whitespace i front/slutt f�r validering og vidare bruk.
     */
    @Test
    public void skalTrimmeBortWhitespaceOgsaaFraaFoedselsdato() {
        final String pnr = "12345";
        final Foedselsnummer expected = new Foedselsnummer("19760503", pnr);
        softly.assertThat(new Foedselsnummer("  19760503", pnr)).isEqualTo(expected);
        softly.assertThat(new Foedselsnummer(" 19760503 ", pnr)).isEqualTo(expected);
        softly.assertThat(new Foedselsnummer("19760503  ", pnr)).isEqualTo(expected);
    }

    @Test
    public void skalSjekkeForNullVerdiarVedKonstruksjon() {
        softly.assertThatThrownBy(() -> new Foedselsnummer(null, "12345"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("F�dselsdato er p�krevd, men var null");
        softly.assertThatThrownBy(() -> new Foedselsnummer("20000101", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Personnummer er p�krevd, men var null");
    }
}