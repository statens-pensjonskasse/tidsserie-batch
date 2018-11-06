package no.spk.felles.tidsserie.batch.core;

import static no.spk.felles.tidsserie.batch.core.medlem.MedlemsId.medlemsId;

import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.medlem.MedlemsId;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class MedlemsIdTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test
    public void skalVerifisereFoedselsdatoSyntaktisk() {
        final String description = "Fødselsdato må vere eit 8-sifra tall";

        softly.assertThatThrownBy(() -> new MedlemsId(
                "1979010101",
                "12345"
        )).as("feil når fødselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");
        softly.assertThatThrownBy(() -> new MedlemsId(
                "    1979010101    ",
                "12345"
        )).as("feil når fødselsdato er for lang")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979010101");

        softly.assertThatThrownBy(() -> new MedlemsId(
                "1979",
                "12345"
        )).as("feil når fødselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");
        softly.assertThatThrownBy(() -> new MedlemsId(
                "1979    ",
                "12345"
        )).as("feil når fødselsdato er for kort")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979");

        softly.assertThatThrownBy(() -> new MedlemsId(
                "1979ABCD",
                "12345"
        )).as("feil når fødselsdato ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 1979ABCD");
        softly.assertThatThrownBy(() -> new MedlemsId(
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

        softly.assertThatThrownBy(() -> new MedlemsId(
                dato,
                "123456"
        )).as("feil når personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");
        softly.assertThatThrownBy(() -> new MedlemsId(
                dato,
                " 123456 "
        )).as("feil når personnummer er for langt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 123456");

        softly.assertThatThrownBy(() -> new MedlemsId(
                dato,
                "ABCDEF"
        )).as("feil når personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var ABCDEF");
        softly.assertThatThrownBy(() -> new MedlemsId(
                dato,
                " ABC "
        )).as("feil når personnummer ikkje er numerisk")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(description)
                .hasMessageEndingWith("var 00ABC");
        softly.assertThatThrownBy(() -> new MedlemsId(
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

        softly.assertThat(new MedlemsId(dato, "1")).isEqualTo(new MedlemsId(dato, "00001"));
        softly.assertThat(new MedlemsId(dato, "10")).isEqualTo(new MedlemsId(dato, "00010"));
        softly.assertThat(new MedlemsId(dato, "100")).isEqualTo(new MedlemsId(dato, "00100"));
        softly.assertThat(new MedlemsId(dato, "1000")).isEqualTo(new MedlemsId(dato, "01000"));
        softly.assertThat(new MedlemsId(dato, "10000")).isEqualTo(new MedlemsId(dato, "10000"));

        softly.assertThat(new MedlemsId(dato, "    1")).isEqualTo(new MedlemsId(dato, "00001"));
        softly.assertThat(new MedlemsId(dato, "   10")).isEqualTo(new MedlemsId(dato, "00010"));
        softly.assertThat(new MedlemsId(dato, "  100")).isEqualTo(new MedlemsId(dato, "00100"));
        softly.assertThat(new MedlemsId(dato, " 1000")).isEqualTo(new MedlemsId(dato, "01000"));
        softly.assertThat(new MedlemsId(dato, "10000")).isEqualTo(new MedlemsId(dato, "10000"));
    }

    /**
     * For å vere konsistent med handteringa av personnummer, verifiser at også fødselsdato blir trimma for
     * whitespace i front/slutt før validering og vidare bruk.
     */
    @Test
    public void skalTrimmeBortWhitespaceOgsaaFraaFoedselsdato() {
        final String pnr = "12345";
        final MedlemsId expected = new MedlemsId("19760503", pnr);
        softly.assertThat(new MedlemsId("  19760503", pnr)).isEqualTo(expected);
        softly.assertThat(new MedlemsId(" 19760503 ", pnr)).isEqualTo(expected);
        softly.assertThat(new MedlemsId("19760503  ", pnr)).isEqualTo(expected);
    }

    @Test
    public void skalSjekkeForNullVerdiarVedKonstruksjon() {
        softly.assertThatThrownBy(
                () -> new MedlemsId(null, "12345")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Fødselsdato er påkrevd, men var null");
        softly.assertThatThrownBy(
                () -> new MedlemsId("20000101", null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Personnummer er påkrevd, men var null");
        softly.assertThatCode(
                () -> new MedlemsId(null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id er påkrevd, men var null");
        softly.assertThatThrownBy(
                () -> medlemsId(null, "12345")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Fødselsdato er påkrevd, men var null");
        softly.assertThatThrownBy(
                () -> medlemsId("20000101", null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Personnummer er påkrevd, men var null");
        softly.assertThatCode(
                () -> medlemsId(null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id er påkrevd, men var null");
    }

    @Test
    public void skal_ikkje_godta_tom_id() {
        softly.assertThatCode(
                () -> medlemsId("")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id er påkrevd, men var tom");
        softly.assertThatCode(
                () -> medlemsId("       ")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id er påkrevd, men var tom");
    }

    @Test
    public void skal_trimme_unødvendig_whitespace_frå_id() {
        softly.assertThat(medlemsId("   Medlemet ")).isEqualTo(medlemsId("Medlemet"));
    }

    @Test
    public void skal_godta_kva_som_helst_som_id() {
        Stream.of("Medlem X", "asiuhgaisgci", "1876182763", "ÆØÅ").forEach(
                verdi -> softly
                        .assertThat(medlemsId(verdi))
                        .hasToString(verdi)
        );
    }

    @Test
    public void skal_ikkje_padde_id() {
        softly.assertThat(medlemsId("123"))
                .isNotEqualTo(medlemsId("00123"))
                .hasToString("123");
    }
}