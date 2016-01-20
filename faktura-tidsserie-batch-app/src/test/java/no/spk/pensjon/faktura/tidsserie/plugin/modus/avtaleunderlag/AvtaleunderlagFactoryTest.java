package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;


import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon.avtaleversjon;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
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

import org.assertj.core.api.OptionalAssert;
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
        final Uttrekksdato uttrekksdato = new Uttrekksdato(dato("2016.01.01"));

        final List<Uttrekksdato> uttrekksdatoer = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, uttrekksdato)
                .map(u -> u.annotasjonFor(Uttrekksdato.class))
                .collect(toList());

        assertThat(uttrekksdatoer).containsExactly(uttrekksdato);
    }


    @Test
    public void skal_annotere_tidsserienummer_paa_underlaget() throws Exception {
        tidsperiodeFactory.addPerioder(enAvtalepriode());

        Tidsserienummer tidsserienummer = Tidsserienummer.genererForDato(now());

        final List<Tidsserienummer> tidsserienummerFraUnderlag = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, new Uttrekksdato(dato("2016.01.01")))
                .map(u -> u.annotasjonFor(Tidsserienummer.class))
                .collect(toList());
        assertThat(tidsserienummerFraUnderlag).containsExactly(tidsserienummer);
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
    public void skal_kunne_bruke_regler_angitt_til_avtaleunderlagfactory() throws Exception {
        underlagFactory = new AvtaleunderlagFactory(tidsperiodeFactory, () -> Stream.of(
                new Regelperiode<>(dato("2000.01.01"), empty(), new AntallDagarRegel())
        ));

        tidsperiodeFactory.addPerioder(enAvtalepriode());

        underlagsperioder()
                .map(p -> p.beregn(AntallDagarRegel.class))
                .forEach(dager -> assertThat(dager.verdi()).isEqualTo(365));
    }

    @Test
    public void skal_lage_underlag_selv_om_avtaleperioder_ikke_overlapper() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                new Avtaleperiode(
                        dato("2015.01.01"),
                        of(dato("2015.01.31")),
                        avtaleId,
                        ArbeidsgiverId.valueOf(2),
                        empty()
                ),
                new Avtaleprodukt(
                        dato("2015.02.01"),
                        of(dato("2015.02.28")),
                        avtaleId,
                        Produkt.PEN,
                        Produktinfo.GRU_35,
                        Satser.ingenSatser()
                ),
                avtaleversjon(avtaleId)
                        .fraOgMed(dato("2015.03.01"))
                        .premiestatus(Premiestatus.AAO_01)
                        .premiekategori(Premiekategori.FASTSATS)
                        .bygg()
        );

        final List<Underlagsperiode> underlagsperioder = underlagsperioder().collect(toList());
        assertThat(underlagsperioder).hasSize(3);
        underlagsperioder.stream().forEach(p -> assertThat(p.valgfriAnnotasjonFor(Avtale.class)).isPresent());

        assertArbeidsgiverid(underlagsperioder.get(0)).isPresent();
        assertArbeidsgiverid(underlagsperioder.get(1)).isEmpty();
        assertArbeidsgiverid(underlagsperioder.get(2)).isEmpty();

        assertPremiesats(underlagsperioder.get(0), Produkt.PEN).isEmpty();
        assertPremiesats(underlagsperioder.get(1), Produkt.PEN).isPresent();
        assertPremiesats(underlagsperioder.get(2), Produkt.PEN).isEmpty();

        assertPremiestatus(underlagsperioder.get(0)).contains(Premiestatus.UKJENT);
        assertPremiestatus(underlagsperioder.get(1)).contains(Premiestatus.UKJENT);
        assertPremiestatus(underlagsperioder.get(2)).contains(Premiestatus.AAO_01);
    }

    private OptionalAssert<Premiestatus> assertPremiestatus(Underlagsperiode underlagsperiode) {
        return assertThat(underlagsperiode.valgfriAnnotasjonFor(Avtale.class).map(Avtale::premiestatus));
    }

    private OptionalAssert<ArbeidsgiverId> assertArbeidsgiverid(Underlagsperiode underlagsperiode) {
        return assertThat(underlagsperiode.valgfriAnnotasjonFor(ArbeidsgiverId.class));
    }

    private OptionalAssert<Premiesats> assertPremiesats(Underlagsperiode underlagsperiode, Produkt produkt) {
        final Optional<Premiesats> premiesats = underlagsperiode.valgfriAnnotasjonFor(Avtale.class).flatMap(a -> a.premiesatsFor(produkt));
        return assertThat(premiesats);
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