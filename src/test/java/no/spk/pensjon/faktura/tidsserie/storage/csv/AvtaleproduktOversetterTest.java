package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent.prosent;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
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
        assertSolskinnslinje("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0.0;0.00",
                new Satser<>(Prosent.ZERO, Prosent.ZERO, prosent("10%")));
    }

    @Test
    public void testSolskinnslinjeKroner() throws Exception {
        assertSolskinnslinje("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;00.00;0;0.0;100.00",
                new Satser<>(Kroner.ZERO, Kroner.ZERO, Kroner.kroner(100)));
    }

    @Test
    public void testSolskinnslinjeIngenSatser() throws Exception {
        assertSolskinnslinje("AVTALEPRODUKT;100001;PEN;2007.01.01;2010.08.31;11;0.00;0.00;00.00;0;0.0;0.00",
                Satser.ingenSatser());
    }

    @Test
    public void testUkjentProduktstrengSkalGiUkjentProduktEnum() throws Exception {
        assertThat(
                oversetter.oversett(
                        asList(
                                "AVTALEPRODUKT;100001;XXX;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0;0".split(";")
                        )
                ).produkt()
        ).isEqualTo(Produkt.UKJ);
    }

    public void assertSolskinnslinje(String csvStreng, Satser<?> satser) throws Exception {
        assertThat(
                oversetter.oversett(
                        asList(csvStreng.split(";")
                        )
                )
        ).isEqualToComparingFieldByField(new Avtaleprodukt(
                LocalDate.of(2007, 1, 1),
                Optional.of(LocalDate.of(2010, 8, 31)),
                AvtaleId.valueOf("100001"),
                Produkt.PEN,
                new Produktinfo(11),
                satser));
    }

    @Test
    public void testProduktlinjeMedPremisatserOgKronestatserKasterFeilmelding() throws Exception {
        e.expect(IllegalStateException.class);
        e.expectMessage("Både prosentsatser og kronesatser kan ikke være i bruk for et avtaleprodukt.");
        oversetter.oversett(
                asList(
                        "AVTALEPRODUKT;100001;XXX;2007.01.01;2010.08.31;11;0.00;0.00;10.00;0;0;2".split(";")
                )
        );
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
}