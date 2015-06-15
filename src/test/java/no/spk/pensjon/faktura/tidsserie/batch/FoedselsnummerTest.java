package no.spk.pensjon.faktura.tidsserie.batch;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class FoedselsnummerTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test
    public void skalVerifisereFoedselsdatoSyntaktisk() {
        final String description = "Fødselsdato må vere eit 8-sifra tall";

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979010101",
                "12345"
        )).as("feil når fødselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "    1979010101    ",
                "12345"
        )).as("feil når fødselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979",
                "12345"
        )).as("feil når fødselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979    ",
                "12345"
        )).as("feil når fødselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "1979ABCD",
                "12345"
        )).as("feil når fødselsdato ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979ABCD");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                "  ABCD  ",
                "12345"
        )).as("feil når fødselsdato ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABCD");
    }

    @Test
    public void skalVerifiserePersonnummerSyntaktisk() {
        final String description = "Personnummer må vere eit 5-sifra tall";
        final String dato = "19790101";

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                "123456"
        )).as("feil når personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " 123456 "
        )).as("feil når personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");

        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                "ABCDEF"
        )).as("feil når personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABCDEF");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " ABC "
        )).as("feil når personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 00ABC");
        softly.assertThatThrownBy(() -> new Foedselsnummer(
                dato,
                " ABC00 "
        )).as("feil når personnummer ikkje er numerisk")
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
     * For å vere konsistent med handteringa av personnummer, verifiser at også fødselsdato blir trimma for
     * whitespace i front/slutt før validering og vidare bruk.
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
                .hasMessageContaining("Fødselsdato er påkrevd, men var null");
        softly.assertThatThrownBy(() -> new Foedselsnummer("20000101", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Personnummer er påkrevd, men var null");
    }
}