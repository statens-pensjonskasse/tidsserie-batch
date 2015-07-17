package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertAdministrasjonsgebyrbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertAdministrasjonsgebyrprosent;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertArbeidsgiverbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertArbeidsgiverprosent;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertMedlemsbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertMedlemsprosent;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale.avtale;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Risikoklasse;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

import javafx.beans.binding.BooleanExpression;
import org.assertj.core.api.OptionalAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleproduktOversetterTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    private final AvtaleproduktOversetter oversetter = new AvtaleproduktOversetter();

    @Test
    public void skalIkkjePopulereRisikoklasseForAndreProduktEnnYsk() {
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;10;0.10;0.01;10.00;0;0;0;1"))
                .isEqualTo(empty());
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;AFP;2007.01.01;2010.08.31;42;0.10;0.01;10.00;0;0;0;2"))
                .isEqualTo(empty());
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;TIP;2007.01.01;2010.08.31;94;0.10;0.01;10.00;0;0;0;3"))
                .isEqualTo(empty());
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;GRU;2007.01.01;2010.08.31;35;0;0;0;535;0;35;3,5"))
                .isEqualTo(empty());
    }

    @Test
    public void skalPopulereRisikoklasseForYsk() {
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;YSK;2007.01.01;2010.08.31;35;0;0;0;535;0;35;3"))
                .isEqualTo(of(new Risikoklasse("3")));
    }

    @Test
    public void skalIkkjeFeileOmRisikoklasseManglar() {
        assertRisikoklasse(oversett("AVTALEPRODUKT;100001;YSK;2007.01.01;2010.08.31;35;0;0;0;535;0;35;"))
                .isEqualTo(empty());
    }

    private OptionalAssert<Risikoklasse> assertRisikoklasse(final Avtaleprodukt produkt) {
        return assertThat(produkt.populer(avtale(produkt.avtale())).bygg().risikoklasse())
                .as("risikoklasse for " + produkt);
    }

    @Test
    public void testSolskinnslinjeProsent() throws Exception {
        final Avtaleprodukt resultat =
                oversett("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.10;0.01;10.00;0;0.0;0.00");

        assertArbeidsgiverprosent(resultat).isEqualTo(of("0,1%"));
        assertMedlemsprosent(resultat).isEqualTo(of("0,01%"));
        assertAdministrasjonsgebyrprosent(resultat).isEqualTo(of("10%"));
    }

    @Test
    public void testSolskinnslinjeKroner() throws Exception {
        final Avtaleprodukt resultat =
                oversett("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;00.00;0;0.0;100.00");

        assertArbeidsgiverbeloep(resultat).isEqualTo(of("kr 0"));
        assertMedlemsbeloep(resultat).isEqualTo(of("kr 0"));
        assertAdministrasjonsgebyrbeloep(resultat).isEqualTo(of("kr 100"));
    }

    @Test
    public void testSolskinnslinjeIngenSatser() throws Exception {
        final Avtaleprodukt actual = oversett("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;00.00;0;0.0;0.00");

        final AvtaleId avtaleId = avtaleId(100001);
        assertThat(actual.fraOgMed()).as("fra og med-dato fra " + actual).isEqualTo(dato("2007.01.01"));
        assertThat(actual.tilOgMed()).as("til og med-dato fra " + actual).isEqualTo(of(dato("2010.08.31")));
        assertThat(actual.avtale()).as("avtale fra " + actual).isEqualTo(avtaleId);
        assertThat(actual.produkt()).as("produkt fra " + actual).isEqualTo(Produkt.PEN);

        final Avtale avtale = actual.populer(avtale(avtaleId)).bygg();
        assertThat(avtale.premiesatsFor(Produkt.PEN)).isNotEqualTo(empty());
        Premiesats premiesats = avtale.premiesatsFor(Produkt.PEN).get();
        assertThat(premiesats.produktinfo).as("produktinfo fra " + actual).isEqualTo(new Produktinfo(11));
        assertThat(premiesats.satser).as("satser fra " + actual).isEqualTo(Satser.ingenSatser());
    }

    @Test
    public void testUkjentProduktstrengSkalGiUkjentProduktEnum() throws Exception {
        assertThat(
                oversett("AVTALEPRODUKT;100001;XXX;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0;0").produkt()
        ).isEqualTo(Produkt.UKJ);
    }

    @Test
    public void testProduktlinjeMedPremisatserOgKronestatserKasterFeilmelding() throws Exception {
        e.expect(IllegalStateException.class);
        e.expectMessage("Både prosentsatser og kronesatser kan ikke være i bruk for et avtaleprodukt.");
        oversett("AVTALEPRODUKT;100001;XXX;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0;2");
    }

    @Test
    public void testForFaaAntallKolonnerGirFeilmelding() throws Exception {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("Rader av typen <AVTALEPRODUKT> må inneholde minimum <12> kolonner, med følgende verdier på angitt index:");
        e.expectMessage("typeindikator(0), avtaleId(1), produkt(2), fraOgMedDato(3), tilOgMedDato(4), produktInfo(5), arbeidsgiverpremieProsent(6), medlemspremieProsent(7), " +
                "administrasjonsgebyrProsent(8), arbeidsgiverpremieBeloep(9), medlemspremieBeloep(10), administrasjonsgebyrBeloep(11)");
        oversetter.oversett(new ArrayList<>());
        e.expectMessage("Både prosentsatser og kronesatser kan ikke være i bruk for et avtaleprodukt.");
    }

    private Avtaleprodukt oversett(final String text) {
        return oversetter.oversett(
                asList(text.split(";", -1)
                )
        );
    }
}