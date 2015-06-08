package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MedlemslinjeTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void skalVerifisereAtInputIkkjeErNull() {
        e.expect(NullPointerException.class);
        e.expectMessage("verdiar for medlemslinja er påkrevd, men var null");

        new Medlemslinje(null);
    }


    @Test
    public void skalPopulereMedlemsIdFraKolonnerForFoedselsdatoOgPersonnummerIkkjeFraMedlemsidentifikator() {
        assertThat(
                new Medlemslinje(
                        asList(
                                "ABCD-BDEF-QXY",
                                "9",
                                "19790101",
                                "12345",
                                "2000.01.01"
                        )
                ).medlem()
        ).as("medlemsidentifikator")
                .isEqualTo(
                        new Foedselsnummer("19790101", "12345")
                );
    }

    /**
     * Verifiserer at medlemsidentifikatoren frå første kolonne i CSV-fila, blir filtrert bort av medlemslinja slik
     * at den ikkje blir med vidare inn i domenemodellen.
     */
    @Test
    public void skalStrippeBortKolonneNr1FraaMedlemslinjasDataliste() {
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
    public void skalKunTilhoyreMedlemmarMedSammeFoedselsnummerSomLinjaErTilknytta() {
        String foedselsdato = "19500405";
        String personnummer = "54321";
        Medlemslinje linje = new Medlemslinje(
                asList(
                        "ABCD",
                        "9",
                        foedselsdato,
                        personnummer,
                        "2019.12.18"
                )
        );
        softlyAssertThat(linje, foedselsdato, personnummer).isTrue();
        softlyAssertThat(linje, foedselsdato, "12345").isFalse();
        softlyAssertThat(linje, "19700806", personnummer).isFalse();
    }

    private BooleanAssert softlyAssertThat(final Medlemslinje linje, final String foedselsdato, final String personnummer) {
        final Foedselsnummer medlem = Foedselsnummer.foedselsnummer(foedselsdato, personnummer);
        return softly.assertThat(linje.tilhoeyrer(medlem)).as("tilhøyrer {" + linje + "} medlem " + medlem + "?");
    }

}