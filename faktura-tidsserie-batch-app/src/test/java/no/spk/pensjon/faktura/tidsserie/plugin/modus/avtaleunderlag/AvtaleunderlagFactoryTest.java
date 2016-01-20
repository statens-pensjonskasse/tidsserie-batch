package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;


import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon.avtaleversjon;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagFactoryTest {

    private PeriodeTypeTestFactory tidsperiodeFactory;
    private AvtaleunderlagFactory underlagFactory;
    private Observasjonsperiode observasjonsperiode;

    @Before
    public void setUp() throws Exception {
        tidsperiodeFactory = new PeriodeTypeTestFactory();
        underlagFactory = new AvtaleunderlagFactory(tidsperiodeFactory, new AvtaleunderlagRegelsett());
        observasjonsperiode = new Observasjonsperiode(dato("2015.01.01"), dato("2015.12.31"));
    }

    @Test
    public void skal_lage_tomt_underlag() throws Exception {
        final List<Underlag> underlag = underlagFactory
                .lagAvtaleunderlag(
                        observasjonsperiode,
                        new Uttrekksdato(dato("2016.01.01"))
                )
                .collect(toList());
        assertThat(underlag).isEmpty();
    }

    @Test
    public void skal_annotere_aarstall() throws Exception {
        tidsperiodeFactory.addPerioder(enAvtalepriode()
        );

        underlagsperioder()
                .map(p -> p.annotasjonFor(Aarstall.class))
                .forEach(aar -> assertThat(aar).isEqualTo(new Aarstall(2015)));
    }

    @Test
    public void skal_annotere_avtale() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                new Avtaleperiode(dato("2015.01.01"),
                        empty(),
                        avtaleId,
                        ArbeidsgiverId.valueOf(2),
                        empty())
        );

        underlagsperioder()
                .map(p -> p.annotasjonFor(Avtale.class))
                .forEach(avtale -> assertThat(avtale.id()).isEqualTo(avtaleId));
    }


    @Test
    public void skal_annotere_premiestatus() throws Exception {
        Premiestatus forventetPremiestatus = Premiestatus.AAO_01;
        tidsperiodeFactory.addPerioder(
                avtaleversjon(avtaleId(1L))
                        .fraOgMed(dato("2015.01.01"))
                        .premiestatus(forventetPremiestatus)
                        .bygg()
        );

        underlagsperioder()
                .map(p -> p.annotasjonFor(Premiestatus.class))
                .forEach(premiestatus -> assertThat(premiestatus).isEqualTo(forventetPremiestatus));
    }

    @Test
    public void skal_annotere_premiekategori() throws Exception {
        Premiekategori forventetPremiekategroi = Premiekategori.FASTSATS;
        tidsperiodeFactory.addPerioder(
                avtaleversjon(avtaleId(1L))
                        .fraOgMed(dato("2015.01.01"))
                        .premiestatus(Premiestatus.AAO_01)
                        .premiekategori(Premiekategori.FASTSATS)
                        .bygg()
        );

        underlagsperioder()
                .map(p -> p.annotasjonFor(Premiekategori.class))
                .forEach(premiekategori -> assertThat(premiekategori).isEqualTo(forventetPremiekategroi));
    }

    @Test
    public void skal_annotere_uttrekksdato() throws Exception {
        tidsperiodeFactory.addPerioder(enAvtalepriode());

        underlagsperioder()
                .map(p -> p.annotasjonFor(Uttrekksdato.class))
                .forEach(uttrekksdato -> assertThat(uttrekksdato).isEqualTo(new Uttrekksdato(dato("2016.01.01"))));
    }


    @Test
    public void skal_annotere_tidsserienummer() throws Exception {
        tidsperiodeFactory.addPerioder(enAvtalepriode());

        Tidsserienummer tidsserienummer = Tidsserienummer.genererForDato(now());

        underlagsperioder()
                .map(p -> p.annotasjonFor(Tidsserienummer.class))
                .forEach(avtale -> assertThat(avtale).isEqualTo(tidsserienummer));
    }

    @Test
    public void avtale_skal_ha_premiesats() throws Exception {
        final Produkt produkt = Produkt.GRU;
        tidsperiodeFactory.addPerioder(
                new Avtaleprodukt(
                        dato("2015.01.01"),
                        empty(),
                        avtaleId(1L),
                        produkt,
                        Produktinfo.GRU_35,
                        Satser.ingenSatser()
                ));

        underlagsperioder()
                .map(p -> p.annotasjonFor(Avtale.class))
                .forEach(avtale -> assertThat(avtale.premiesatsFor(produkt)).isPresent());
    }

    @Test
    public void skal_beregne_regler_som_er_benyttet() throws Exception {
        underlagFactory = new AvtaleunderlagFactory(tidsperiodeFactory, () -> Stream.of(
                new Regelperiode<>(dato("2000.01.01"), empty(), new AntallDagarRegel())
        ));

        tidsperiodeFactory.addPerioder(enAvtalepriode());

        underlagsperioder()
                .map(p -> p.beregn(AntallDagarRegel.class))
                .forEach(dager -> assertThat(dager.verdi()).isEqualTo(365));
    }

    private Avtaleperiode enAvtalepriode() {
        return new Avtaleperiode(dato("2015.01.01"),
                empty(),
                avtaleId(1L),
                ArbeidsgiverId.valueOf(2),
                empty());
    }


    private Stream<Underlagsperiode> underlagsperioder() {
        final List<Underlagsperiode> perioder = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, new Uttrekksdato(dato("2016.01.01")))
                .flatMap(Underlag::stream)
                .collect(toList());
        assertThat(perioder.size())
                .as("underlaget må inneholde minst én underlagsperiode, men underlaget hadde ingen perioder")
                .isGreaterThan(0);
        return perioder.stream();
    }


}