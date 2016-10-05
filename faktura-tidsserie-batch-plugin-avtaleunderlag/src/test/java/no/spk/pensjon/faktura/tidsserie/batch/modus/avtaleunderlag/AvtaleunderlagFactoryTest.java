package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;


import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag.Optionals.stream;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode.avtaleperiode;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon.avtaleversjon;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.felles.tidsperiode.underlag.reglar.Regelperiode;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.TreigUUIDRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.UUIDRegel;

import ch.qos.logback.classic.Level;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.OptionalAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagFactoryTest {
    @Rule
    public final LogbackVerifier logback = new LogbackVerifier();

    private PeriodeTypeTestFactory tidsperiodeFactory;
    private AvtaleunderlagFactory underlagFactory;
    private Observasjonsperiode observasjonsperiode;
    private Context context = new Context();

    @Before
    public void setUp() throws Exception {
        tidsperiodeFactory = new PeriodeTypeTestFactory();
        underlagFactory = new AvtaleunderlagFactory(tidsperiodeFactory, new AvtaleunderlagRegelsett());
        observasjonsperiode = new Observasjonsperiode(dato("2015.01.01"), dato("2015.12.31"));
    }

    @Test
    public void skal_annotere_underlagsperioder_med_uuid() {
        tidsperiodeFactory.addPerioder(
                enAvtaleperiode()
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.01.31"))
                        .bygg()
                ,
                enAvtaleperiode()
                        .fraOgMed(dato("2015.02.01"))
                        .tilOgMed(dato("2015.12.31"))
                        .bygg()
        );

        assertThat(
                underlagsperioder()
                        .map(p -> p.valgfriAnnotasjonFor(UUID.class))
                        .collect(toList())
        )
                .as("UUID-annotasjoner")
                .hasSize(12)
                .filteredOn(Optional::isPresent)
                .extracting(Optional::get)
                .hasSize(12)
                .doesNotHaveDuplicates()
        ;
    }

    @Test
    public void skal_lage_tomt_underlag() throws Exception {
        final LocalDate tilOgMed = dato("2015.12.31");
        final List<Underlag> underlag = underlagFactory
                .lagAvtaleunderlag(
                        new Observasjonsperiode(dato("2015.01.01"), tilOgMed),
                        new Uttrekksdato(tilOgMed.plusDays(1)),
                        context
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

    /**
     * Verifiserer at avtaleunderlaget er splitta i perioder pr måned, ikkje kun pr år eller pr endring i avtale eller
     * arbeidsgivar.
     * <br>
     * Hovedårsaka til at ein ønskjer å splitte avtaleunderlaget pr måned er at ein i saksbehandlerdashoardet til FFF
     * skal kunne
     * sammenstille avtaleunderlaget med live_tidsserien for å telle antall ansatte pr avtale pr siste dag i måned
     * (f.eks.).
     */
    @Test
    public void skal_splitte_perioder_paa_maaned_selv_om_ingenting_annet_endres() {
        tidsperiodeFactory.addPerioder(
                enAvtaleperiode()
                        .fraOgMed(dato("2016.01.01"))
                        .bygg()
        );

        assertThat(
                underlagsperioder(observasjonsperiode(2016, 2016))
                        .flatMap(up -> stream(up.valgfriAnnotasjonFor(Month.class)))
        )
                .containsExactly(
                        Month.values()
                );
    }

    @Test
    public void skal_annotere_avtale() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtaleId)
                        .fraOgMed(dato("2015.01.01"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg()
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
                .lagAvtaleunderlag(observasjonsperiode, uttrekksdato, context)
                .map(u -> u.annotasjonFor(Uttrekksdato.class))
                .collect(toList());

        Assertions.assertThat(uttrekksdatoer).containsExactly(uttrekksdato);
    }


    @Test
    public void skal_annotere_tidsserienummer_paa_underlaget() throws Exception {
        tidsperiodeFactory.addPerioder(enAvtalepriode());

        Tidsserienummer tidsserienummer = Tidsserienummer.genererForDato(now());

        final List<Tidsserienummer> tidsserienummerFraUnderlag = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, new Uttrekksdato(dato("2016.01.01")), context)
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
                new Regelperiode<>(dato("2000.01.01"), empty(), new AntallDagarRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), UUIDRegel.class, new TreigUUIDRegel())
        ));

        tidsperiodeFactory.addPerioder(enAvtalepriode());

        underlagsperioder()
                .forEach(p ->
                        assertThat(
                                p.beregn(AntallDagarRegel.class)
                                        .verdi()
                        )
                                .as("Resultat frå AntallDagarRegel for " + p)
                                .isEqualTo(
                                        p
                                                .annotasjonFor(Month.class)
                                                .length(p.fraOgMed().isLeapYear())
                                )
                );
    }

    @Test
    public void skal_lage_underlag_selv_om_avtaleperioder_ikke_overlapper() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtaleId)
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.01.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg(),
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
                        .tilOgMed(of(dato("2015.03.31")))
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

    /*  Verifiserer at avtaleunderlage genereres selv om en eller flere avtaleproduktperioder ikke har
        andre overlappande avtaleversjoner eller avtaleperioder */
    @Test
    public void skal_lage_underlag_selv_om_avtaleprodukter_ikke_har_andre_overlappende_perioder() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                new Avtaleprodukt(
                        dato("2015.12.01"),
                        empty(),
                        avtaleId(avtaleId.id()),
                        Produkt.GRU,
                        Produktinfo.GRU_35,
                        new Satser<>(kroner(2), kroner(20), kroner(200))),
                new Avtaleprodukt(
                        dato("2015.12.01"),
                        empty(),
                        avtaleId(avtaleId.id()),
                        Produkt.YSK,
                        Produktinfo.YSK_79,
                        new Satser<>(kroner(0), kroner(0), kroner(0)))
        );
        final List<Underlag> underlag = underlagFactory
                .lagAvtaleunderlag(
                        observasjonsperiode,
                        new Uttrekksdato(dato("2016.01.01")),
                        context
                )
                .collect(toList());
        assertThat(underlag).isNotEmpty();
    }

    @Test
    public void skal_lage_underlag_selv_om_avtaler_feiler_pga_overlappende_perioder() {
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtaleId(1L))
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.03.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(1))
                        .bygg(),
                avtaleperiode(avtaleId(2L))
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.03.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg(),
                avtaleperiode(avtaleId(2L))
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.01.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg()
        );
        final List<Underlag> underlag = underlagFactory
                .lagAvtaleunderlag(
                        observasjonsperiode,
                        new Uttrekksdato(dato("2016.01.01")),
                        context
                )
                .collect(toList());
        assertThat(underlag).hasSize(1);
    }

    @Test
    public void skal_lage_underlag_selv_om_avtaler_feiler_pga_gap_i_perioder() {
        AvtaleId avtale1 = new AvtaleId(12345L);
        AvtaleId avtale2 = new AvtaleId(12346L);
        AvtaleId avtale3 = new AvtaleId(12347L);
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtale1)
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.03.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(1))
                        .bygg(),
                avtaleperiode(avtale2)
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.01.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(1))
                        .bygg(),
                avtaleperiode(avtale2)
                        .fraOgMed(dato("2015.03.01"))
                        .tilOgMed(dato("2015.03.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg(),
                avtaleperiode(avtale3)
                        .fraOgMed(dato("2015.01.01"))
                        .tilOgMed(dato("2015.12.31"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(1))
                        .bygg()
        );
        final List<Underlag> underlag = underlagFactory
                .lagAvtaleunderlag(
                        observasjonsperiode,
                        new Uttrekksdato(dato("2016.01.01")),
                        context
                )
                .collect(toList());
        assertThat(underlag).hasSize(2);
        assertThat(underlag.get(0).valgfriAnnotasjonFor(AvtaleId.class)).isEqualTo(of(avtale1));
        assertThat(underlag.get(1).valgfriAnnotasjonFor(AvtaleId.class)).isEqualTo(of(avtale3));
    }

    @Test
    public void skal_hente_orgnummer_fra_arbeidsgiverperiode_via_arbeidsgiverid_i_avtaleperiode() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        final ArbeidsgiverId arbeidsgiverId = ArbeidsgiverId.valueOf(2);
        final AvtaleId avtaleUtenArbeidsgiverperiode = AvtaleId.avtaleId(2L);
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtaleId)
                        .fraOgMed(dato("2015.01.01"))
                        .arbeidsgiverId(arbeidsgiverId)
                        .bygg(),
                avtaleperiode(avtaleUtenArbeidsgiverperiode)
                        .fraOgMed(dato("2015.01.01"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(50))
                        .bygg(),
                new Arbeidsgiverdataperiode(
                        dato("2015.01.01"),
                        empty(),
                        Orgnummer.valueOf("999222111"),
                        arbeidsgiverId
                )
        );

        final Map<AvtaleId, List<Underlagsperiode>> avtaleunderlag = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, new Uttrekksdato(dato("2016.01.01")), context)
                .collect(toMap(
                        u -> u.annotasjonFor(AvtaleId.class),
                        u -> u.stream().collect(toList())
                        )
                );

        assertThat(avtaleunderlag).hasSize(2);
        assertThat(avtaleunderlag.get(avtaleId)).hasSize(12);
        assertThat(avtaleunderlag.get(avtaleUtenArbeidsgiverperiode)).hasSize(12);

        avtaleunderlag
                .get(avtaleId)
                .forEach(up -> assertOrgnummer(up).isPresent());
        avtaleunderlag
                .get(avtaleUtenArbeidsgiverperiode)
                .forEach(up -> assertOrgnummer(up).isEmpty());
    }

    private OptionalAssert<Premiestatus> assertPremiestatus(Underlagsperiode underlagsperiode) {
        return assertThat(underlagsperiode.valgfriAnnotasjonFor(Avtale.class).map(Avtale::premiestatus));
    }

    private OptionalAssert<ArbeidsgiverId> assertArbeidsgiverid(Underlagsperiode underlagsperiode) {
        return assertThat(underlagsperiode.valgfriAnnotasjonFor(ArbeidsgiverId.class));
    }

    private OptionalAssert<Orgnummer> assertOrgnummer(Underlagsperiode underlagsperiode) {
        return assertThat(underlagsperiode.valgfriAnnotasjonFor(Orgnummer.class));
    }

    private OptionalAssert<Premiesats> assertPremiesats(Underlagsperiode underlagsperiode, Produkt produkt) {
        final Optional<Premiesats> premiesats = underlagsperiode.valgfriAnnotasjonFor(Avtale.class).flatMap(a -> a.premiesatsFor(produkt));
        return assertThat(premiesats);
    }

    private Avtaleperiode enAvtalepriode() {
        return enAvtaleperiode().bygg();
    }

    private static Avtaleperiode.AvtaleperiodeBuilder enAvtaleperiode() {
        return avtaleperiode(avtaleId(1L))
                .fraOgMed(dato("2015.01.01"))
                .arbeidsgiverId(ArbeidsgiverId.valueOf(2));
    }

    private static Observasjonsperiode observasjonsperiode(final int fraAar, final int tilAar) {
        return new Observasjonsperiode(
                new Aarstall(fraAar).atStartOfYear(),
                new Aarstall(tilAar).atEndOfYear()
        );
    }

    private Stream<Underlagsperiode> underlagsperioder() {
        return underlagsperioder(this.observasjonsperiode);
    }

    private Stream<Underlagsperiode> underlagsperioder(final Observasjonsperiode observasjonsperiode) {
        final List<Underlagsperiode> perioder = underlagFactory
                .lagAvtaleunderlag(observasjonsperiode, new Uttrekksdato(dato("2016.01.01")), context)
                .flatMap(Underlag::stream)
                .collect(toList());
        assertThat(perioder.size())
                .as("underlaget må inneholde minst én underlagsperiode, men underlaget hadde ingen perioder")
                .isGreaterThan(0);
        return perioder.stream();
    }
}