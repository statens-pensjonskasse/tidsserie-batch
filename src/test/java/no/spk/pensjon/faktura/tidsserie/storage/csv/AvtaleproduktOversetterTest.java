package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertAdministrasjonsgebyrbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertArbeidsgiverbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Assertions.assertMedlemsbeloep;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.valueOf;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent.prosent;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

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
    public void testSolskinnslinjeProsent() throws Exception {
        final Avtaleprodukt resultat =
                oversett("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0.0;0.00");

        assertArbeidsgiverbeloep(resultat).isEqualTo(of("0%"));
        assertMedlemsbeloep(resultat).isEqualTo(of("0%"));
        assertAdministrasjonsgebyrbeloep(resultat).isEqualTo(of("10%"));
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
        assertSolskinnslinje("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;00.00;0;0.0;0.00",
                Satser.ingenSatser());
    }

    @Test
    public void testUkjentProduktstrengSkalGiUkjentProduktEnum() throws Exception {
        assertThat(
                oversett("AVTALEPRODUKT;100001;XXX;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0;0").produkt()
        ).isEqualTo(Produkt.UKJ);
    }

    public void assertSolskinnslinje(String csvStreng, Satser<?> satser) throws Exception {
        assertThat(
                oversett(csvStreng)
        ).isEqualToComparingFieldByField(new Avtaleprodukt(
                LocalDate.of(2007, 1, 1),
                of(LocalDate.of(2010, 8, 31)),
                valueOf("100001"),
                Produkt.PEN,
                new Produktinfo(11),
                satser));
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
        e.expectMessage("typeindikator, avtaleid, produkt, fra og med-dato, til og med-dato, produktinfo, " +
                "arbeidsgiverpremie prosent, medlemspremie prosent, administrasjongebyr prosent, " +
                "arbeidsgiverpremie beløp, medlemspremie beløp, administrasjongebyr beløp");
        oversetter.oversett(new ArrayList<>());
        e.expectMessage("Både prosentsatser og kronesatser kan ikke være i bruk for et avtaleprodukt.");
    }

    private Avtaleprodukt oversett(final String text) {
        return oversetter.oversett(
                asList(text.split(";")
                )
        );
    }
}