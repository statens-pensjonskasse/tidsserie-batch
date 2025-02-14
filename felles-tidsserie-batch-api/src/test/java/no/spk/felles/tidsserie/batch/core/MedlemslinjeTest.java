package no.spk.felles.tidsserie.batch.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.spk.felles.tidsserie.batch.core.medlem.MedlemsId.medlemsId;
import static org.assertj.core.api.Assertions.assertThat;

import no.spk.felles.tidsserie.batch.core.medlem.MedlemsId;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;

import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class MedlemslinjeTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void skal_godta_kva_som_helst_utenom_ingenting_i_kolonne_1() {
        final String id = "Medlem X";
        final Medlemslinje linje = new Medlemslinje(
                asList(id, "18763187263", "127631827631872")
        );
        assertThat(linje.medlem()).isEqualTo(new MedlemsId(id));
        assertThat(linje.data()).isEqualTo(asList("18763187263", "127631827631872"));
    }

    @Test
    void skal_ikkje_kreve_fleire_kolonner_enn_1() {
        final String id = "Medlem Y";
        final Medlemslinje linje = new Medlemslinje(
                singletonList(id)
        );
        assertThat(linje.medlem()).isEqualTo(new MedlemsId(id));
        assertThat(linje.data()).isEmpty();
    }

    @Test
    void skal_ikkje_godta_tomme_linjer() {
        softly.assertThatCode(
                () -> new Medlemslinje(emptyList())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Ei medlemslinje må inneholde minst 1 kolonne med medlemsdata, antall kolonner var 0\n" +
                                "Kolonner:\n"
                );
    }

    @Test
    void skal_ikkje_godta_null_som_medlemsid() {
        softly.assertThatCode(
                () -> new Medlemslinje(asList(null, "ABCD"))
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Kolonne for medlemsid kan ikkje inneholde verdien '<null>'")
                .hasMessageContaining("Kolonner:")
                .hasMessageContaining("- <null>")
                .hasMessageContaining("- ABCD");
    }

    @Test
    void skal_ikkje_godta_tom_string_som_medlemsid() {
        softly.assertThatCode(
                () -> new Medlemslinje(asList("  ", "ABCD"))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kolonne for medlemsid kan ikkje inneholde verdien '<tom streng>'")
                .hasMessageContaining("Kolonner:")
                .hasMessageContaining("- <tom streng>")
                .hasMessageContaining("- ABCD");
    }

    @Test
    void skal_trimme_medlemsidentifikatoren() {
        assertThat(
                new Medlemslinje(singletonList("   Medlem W "))
                        .medlem()
        )
                .isEqualTo(medlemsId("Medlem W"));
    }

    @Test
    void skal_godta_tom_string_i_medlemsdata_kolonnene_etter_medlemsid() {
        assertThat(
                new Medlemslinje(asList("Medlem W", "A", "  ", ""))
                        .data()
        )
                .isEqualTo(asList("A", "  ", ""));
    }

    @Test
    void skalVerifisereAtInputIkkjeErNull() {
        softly.assertThatCode(
                () -> new Medlemslinje(null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("verdiar for medlemslinja er påkrevd, men var null")
        ;
    }


    @Test
    void skal_populere_medlemsId_fra_kolonne_for_medlemsidentifikator() {
        final String expected = "ABCD-BDEF-QXY";
        assertThat(
                new Medlemslinje(
                        asList(
                                expected,
                                "9",
                                "19790101",
                                "12345",
                                "2000.01.01"
                        )
                )
                        .medlem()
        )
                .as("medlemsidentifikator")
                .isEqualTo(
                        new MedlemsId(expected)
                );
    }

    /**
     * Verifiserer at medlemsidentifikatoren frå første kolonne i CSV-fila, blir filtrert bort av medlemslinja slik
     * at den ikkje blir med vidare inn i domenemodellen.
     */
    @Test
    void skalStrippeBortKolonneNr1FraaMedlemslinjasDataliste() {
        assertThat(
                new Medlemslinje(
                        asList(
                                "ABCD-BDEF-QXY",
                                "9",
                                "19790101",
                                "12345",
                                "9",
                                "2000.01.01"
                        )
                ).data()
        ).as("medlemsdata")
                .doesNotContain("ABCD-BDEF-QXY");
    }

    @Test
    void skal_kun_tilhøyre_linjer_med_samme_id() {
        final Medlemslinje linje = new Medlemslinje(singletonList("ABCD"));
        assertTilhøyrer(linje, "ABCD").isTrue();
        assertTilhøyrer(linje, "12345").isFalse();
    }

    private BooleanAssert assertTilhøyrer(final Medlemslinje linje, final String id) {
        final MedlemsId other = medlemsId(id);
        return softly.assertThat(
                linje.tilhoeyrer(other)
        )
                .as("tilhøyrer {%s} medlem %s", linje, other);
    }

}