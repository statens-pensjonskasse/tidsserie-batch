package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer.genererForDato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale.avtale;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato.foedselsdato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn.loennstrinn;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer.personnummer;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats.premiesats;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.AFP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.GRU;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.PEN;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.TIP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.YSK;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent.prosent;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId.stillingsforhold;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent.fulltid;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Termintype;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AktiveStillingar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.DeltidsjustertLoenn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Fastetillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Funksjonstillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Grunnbeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.LoennstrinnBeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medlemsavtalar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medlemslinjenummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Risikoklasse;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.prognose.PrognoseRegelsett;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;
import no.spk.felles.tidsperiode.underlag.UnderlagsperiodeBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

/**
 * Integrasjonstest som verifiserer at {@link Datavarehusformat} genererer tekstlige verdiar på forventa format.
 * <br>
 * Testen blir satt opp med eit parametrisert sett med forventningar for kvar kolonne og verifiserer ut frå dette
 * at gitt ein bestemt input så blir den tekstlige verdien som kjem ut av mappinga, generert med rett format.
 *
 * @author Tarjei Skorgenes
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(Parameterized.class)
public class DatavarehusformatMappingTest {
    private static final Premiesats.Builder fakturerbartPensjonsprodukt = premiesats(PEN).produktinfo(new Produktinfo(10)).satser(new Satser<>(prosent("10%"), prosent("2%"), prosent("0.35%")));
    private static final Premiesats.Builder fakturerbartAFPprodukt = premiesats(AFP).produktinfo(new Produktinfo(40)).satser(new Satser<>(prosent("2%"), prosent("0%"), prosent("0%")));
    private static final Premiesats.Builder fakturerbartTIPprodukt = premiesats(TIP).produktinfo(new Produktinfo(94)).satser(new Satser<>(prosent("-20%"), prosent("0%"), prosent("0%")));
    private static final Premiesats.Builder fakturerbartGRUprodukt = premiesats(GRU).produktinfo(new Produktinfo(35)).satser(new Satser<>(kroner(535), kroner(0), kroner(35)));
    private static final Premiesats.Builder fakturerbartYSKprodukt = premiesats(YSK).produktinfo(new Produktinfo(71)).satser(new Satser<>(kroner(2535), kroner(0), kroner(35)));

    @Parameterized.Parameters(name = "kolonne={0},type={1}")
    public static List<Object[]> parameters() {
        return Arrays.<Object[]>asList(
                instance(kolonne(1), Observasjonsdato.class, new Observasjonsdato(dato("2015.04.30")), forventa("2015-04-30")),
                instance(kolonne(2), FraOgMedDato.class, dato("2012.02.01"), forventa("2012-02-01")),
                instance(kolonne(3), TilOgMedDato.class, dato("2012.02.29"), forventa("2012-02-29")),
                instance(kolonne(4), Foedselsnummer.class, new Foedselsnummer(foedselsdato(19790806), personnummer(32817)), forventa("1979080632817")),
                instance(kolonne(5), StillingsforholdId.class, stillingsforhold(287278692), forventa("287278692")),
                instance(kolonne(6), AvtaleId.class, avtaleId(282762), forventa("282762")),
                instance(kolonne(7), Orgnummer.class, new Orgnummer(123456789L), forventa("123456789")),
                instance(kolonne(8), Ordning.class, Ordning.OPERA, forventa("3035")),
                instance(kolonne(9), Premiestatus.class, Premiestatus.UKJENT, forventa("UKJENT")),
                instance(kolonne(10), Aksjonskode.class, Aksjonskode.ENDRINGSMELDING, forventa("021")),
                instance(kolonne(11), Stillingskode.class, Stillingskode.K_STIL_APO_GENERALSEKRETER, forventa("14")),
                instance(kolonne(12), Stillingsprosent.class, fulltid(), forventa("100.000")),
                instance(kolonne(13), Loennstrinn.class, loennstrinn(100), forventa("100")),
                instance(kolonne(14), LoennstrinnBeloep.class, new LoennstrinnBeloep(kroner(1_050_123)), forventa("1050123")),
                instance(kolonne(15), DeltidsjustertLoenn.class, new DeltidsjustertLoenn(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(16), Fastetillegg.class, new Fastetillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(17), Variabletillegg.class, new Variabletillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(18), Funksjonstillegg.class, new Funksjonstillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(19), Medregning.class, new Medregning(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(20), Medregningskode.class, Medregningskode.TILLEGG_ANNEN_ARBGIV, forventa("14")),
                instance(kolonne(21), Grunnbeloep.class, new Grunnbeloep(kroner(120_987)), forventa("120987")),
                // TODO: Reglar, dei blir meir komplekse oppsettsmessig sidan dei treng langt meir tilstand pr regel
                instance(kolonne(37), Avtale.class, einPremiesats(eitPensjonsprodukt()), forventa("1")),
                instance(kolonne(38), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(new Satser(prosent("999.0051%"), Prosent.ZERO, Prosent.ZERO))), forventa("999.01")),
                instance(kolonne(39), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(new Satser(Prosent.ZERO, prosent("999.5551%"), Prosent.ZERO))), forventa("999.56")),
                instance(kolonne(40), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(new Satser(Prosent.ZERO, Prosent.ZERO, prosent("0.354%")))), forventa("0.35")),
                instance(kolonne(41), Avtale.class, einPremiesats(eitPensjonsprodukt().produktinfo(new Produktinfo(10))), forventa("10")),
                instance(kolonne(42), Avtale.class, einPremiesats(eitAFPprodukt()), forventa("1")),
                instance(kolonne(43), Avtale.class, einPremiesats(eitAFPprodukt().satser(new Satser(prosent("2%"), Prosent.ZERO, Prosent.ZERO))), forventa("2.00")),
                instance(kolonne(44), Avtale.class, einPremiesats(eitAFPprodukt().satser(new Satser(Prosent.ZERO, prosent("200%"), Prosent.ZERO))), forventa("200.00")),
                instance(kolonne(45), Avtale.class, einPremiesats(eitAFPprodukt().satser(new Satser(Prosent.ZERO, Prosent.ZERO, prosent("0%")))), forventa("0.00")),
                instance(kolonne(46), Avtale.class, einPremiesats(eitAFPprodukt().produktinfo(new Produktinfo(42))), forventa("42")),
                instance(kolonne(47), Avtale.class, einPremiesats(eitTIPprodukt()), forventa("1")),
                instance(kolonne(48), Avtale.class, einPremiesats(eitTIPprodukt().satser(new Satser<>(prosent("108.234%"), Prosent.ZERO, Prosent.ZERO))), forventa("108.23")),
                instance(kolonne(49), Avtale.class, einPremiesats(eitTIPprodukt().satser(new Satser<>(Prosent.ZERO, prosent("-744.499%"), Prosent.ZERO))), forventa("-744.50")),
                instance(kolonne(50), Avtale.class, einPremiesats(eitTIPprodukt().satser(new Satser<>(Prosent.ZERO, Prosent.ZERO, prosent("0.999%")))), forventa("1.00")),
                instance(kolonne(51), Avtale.class, einPremiesats(eitTIPprodukt().produktinfo(new Produktinfo(95))), forventa("95")),
                instance(kolonne(52), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(new Produktinfo(31))), forventa("0")),
                instance(kolonne(52), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(Produktinfo.GRU_36)), forventa("1")),
                instance(kolonne(53), Avtale.class, einPremiesats(eitGRUprodukt().satser(new Satser<>(kroner(99999), Kroner.ZERO, Kroner.ZERO))), forventa("99999")),
                instance(kolonne(54), Avtale.class, einPremiesats(eitGRUprodukt().satser(new Satser<>(Kroner.ZERO, kroner(923), Kroner.ZERO))), forventa("923")),
                instance(kolonne(55), Avtale.class, einPremiesats(eitGRUprodukt().satser(new Satser<>(Kroner.ZERO, Kroner.ZERO, kroner(17)))), forventa("17")),
                instance(kolonne(56), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(new Produktinfo(39))), forventa("39")),
                instance(kolonne(57), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(Produktinfo.YSK_79)), forventa("0")),
                instance(kolonne(57), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(new Produktinfo(71))), forventa("1")),
                instance(kolonne(58), Avtale.class, einPremiesats(eitYSKprodukt().satser(new Satser<>(kroner(4500), Kroner.ZERO, Kroner.ZERO))), forventa("4500")),
                instance(kolonne(59), Avtale.class, einPremiesats(eitYSKprodukt().satser(new Satser<>(Kroner.ZERO, kroner(450), Kroner.ZERO))), forventa("450")),
                instance(kolonne(60), Avtale.class, einPremiesats(eitYSKprodukt().satser(new Satser<>(Kroner.ZERO, Kroner.ZERO, kroner(-45)))), forventa("-45")),
                instance(kolonne(61), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(new Produktinfo(70))), forventa("70")),
                instance(kolonne(62), Avtale.class, einAvtale(eitYSKprodukt()).risikoklasse(of(new Risikoklasse("1,5"))).bygg(), forventa("1,5")),
                instance(kolonne(63), UUID.class, UUID.fromString("12345678-FEDC-BA09-8765-432101234567"), forventa("12345678-fedc-ba09-8765-432101234567")),
                instance(kolonne(64), Feilantall.class, null, forventa("0")),
                instance(kolonne(65), ArbeidsgiverId.class, new ArbeidsgiverId(100_000L), forventa("100000")),
                instance(kolonne(66), Tidsserienummer.class, genererForDato(dato("2016.01.07")), forventa("20160107")),
                instance(kolonne(67), Termintype.class, Termintype.UKJENT, forventa("UKJ")),
                instance(kolonne(68), Medlemslinjenummer.class, Medlemslinjenummer.linjenummer(18), forventa("18")),
                instance(kolonne(69), Premiekategori.class, Premiekategori.HENDELSESBASERT, forventa("LOP"))
        );
    }

    private static Premiesats.Builder eitPensjonsprodukt() {
        return fakturerbartPensjonsprodukt.kopi();
    }

    private static Premiesats.Builder eitAFPprodukt() {
        return fakturerbartAFPprodukt.kopi();
    }

    private static Premiesats.Builder eitTIPprodukt() {
        return fakturerbartTIPprodukt.kopi();
    }

    private static Premiesats.Builder eitGRUprodukt() {
        return fakturerbartGRUprodukt.kopi();
    }

    private static Premiesats.Builder eitYSKprodukt() {
        return fakturerbartYSKprodukt.kopi();
    }

    private static Avtale einPremiesats(final Premiesats.Builder premiesats) {
        return einAvtale(premiesats)
                .bygg();
    }

    private static Avtale.AvtaleBuilder einAvtale(final Premiesats.Builder premiesats) {
        return avtale(avtaleId(123456L))
                .addPremiesats(
                        premiesats.bygg()
                );
    }

    @Parameter(0)
    public int kolonneNr;

    @Parameter(1)
    public Class<?> fieldType;

    @Parameter(2)
    public Object value;

    @Parameter(3)
    public Forventning forventning;

    @Parameter(4)
    public UnderlagsperiodeBuilder builder;

    private final Datavarehusformat format = new Datavarehusformat();

    @Test
    public void skalHenteUtForventaVerdiFraaPerioda() {
        final Underlag underlag = populer(builder);
        final Stream<Object> values = format.serialiser(underlag, underlag.last().get());

        assertThat(values.skip(kolonneNr - 1).findFirst().get())
                .as("kolonneverdi for type " + fieldType.getSimpleName() + " fra kolonne " + kolonneNr)
                .matches(forventning.matcher, forventning.message);
    }

    private Underlag populer(UnderlagsperiodeBuilder builder) {
        Object consumer = populators.get(fieldType);

        // Anta verdien kjem frå ein annotasjon på perioda viss ingen populator er lagt til
        if (consumer == null) {
            builder.med(fieldType, value);
            return eitUnderlag(builder);
        }
        if (consumer instanceof UnderlagConsumer) {
            final UnderlagConsumer c = (UnderlagConsumer) consumer;
            return populerFraUnderlag(builder, c);
        } else if (consumer instanceof UnderlagsperiodeConsumer) {
            final UnderlagsperiodeConsumer c = (UnderlagsperiodeConsumer) consumer;
            return populerFraPeriode(builder, c);
        }
        return null;
    }

    private Underlag populerFraPeriode(UnderlagsperiodeBuilder builder, UnderlagsperiodeConsumer c) {
        c.accept(value, builder);
        return eitUnderlag(builder);
    }

    private Underlag populerFraUnderlag(UnderlagsperiodeBuilder builder, UnderlagConsumer c) {
        final Underlag underlag = eitUnderlag(builder);
        c.accept(value, underlag);
        return underlag;
    }

    private Underlag eitUnderlag(UnderlagsperiodeBuilder builder) {
        final Underlagsperiode underlagsperiode = builder.bygg();
        new PrognoseRegelsett().reglar().forEach(r -> r.annoter(underlagsperiode));
        final Underlag underlag = new Underlag(Stream.of(underlagsperiode));
        underlag.annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.12.31")));
        return underlag;
    }

    private static UnderlagsperiodeBuilder eiPeriode() {
        return new UnderlagsperiodeBuilder()
                .fraOgMed(dato("2012.01.01"))
                .tilOgMed(dato("2012.12.31"))
                .med(new Foedselsnummer(foedselsdato(19800101), personnummer(1)))
                .med(stillingsforhold(1L))
                .med(avtaleId(223344L))
                .med(Ordning.POA)
                .med(Premiestatus.valueOf("AAO-12"))
                .med(Aksjonskode.ENDRINGSMELDING)
                .med(Stillingskode.K_STIL_APO_RESEPTAR)
                .med(new Stillingsprosent(prosent("100%")))
                .med(loennstrinn(70))
                .med(new LoennstrinnBeloep(kroner(650_000)))
                .med(new DeltidsjustertLoenn(kroner(600_000)))
                .med(new Fastetillegg(kroner(0)))
                .med(new Variabletillegg(kroner(0)))
                .med(new Funksjonstillegg(kroner(0)))
                .med(new Medregning(kroner(700_000)))
                .med(Medregningskode.BISTILLING)
                .med(new Grunnbeloep(kroner(88_370)))
                .med(new Aarstall(2000))
                .med(Month.JANUARY)
                .med(AktiveStillingar.class, Stream::empty)
                .med(Medlemsavtalar.class, new Medlemsavtalar() {
                    @Override
                    public boolean betalarTilSPKFor(final StillingsforholdId stilling, final Produkt produkt) {
                        return false;
                    }

                    @Override
                    public Avtale avtaleFor(final StillingsforholdId stilling) {
                        throw new UnsupportedOperationException();
                    }
                })
                .med(UUID.randomUUID())
                ;
    }

    private static int kolonne(int nr) {
        return nr;
    }

    private static Forventning forventa(final String verdi) {
        return new Forventning(v -> v.equals(verdi)).as("is equal to " + verdi);
    }

    private static Object[] instance(final Object... args) {
        if (args.length == 4) {
            return Stream.concat(
                    stream(args),
                    Stream.of(eiPeriode())
            ).toArray();
        }
        return args;
    }

    @SuppressWarnings("serial")
    private static Map<Object, Object> populators = new HashMap<Object, Object>() {
        {
            put(Observasjonsdato.class, fraUnderlag((Observasjonsdato value, Underlag underlag) -> underlag.annoter(Observasjonsdato.class, value)));
            put(FraOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.fraOgMed(value)));
            put(TilOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.tilOgMed(value)));
            put(Feilantall.class, ignorer());
            put(Tidsserienummer.class, fraUnderlag((Tidsserienummer value, Underlag underlag) -> underlag.annoter(Tidsserienummer.class, value)));
        }

        private <T> UnderlagConsumer<T> fraUnderlag(final UnderlagConsumer<T> value) {
            return value;
        }

        private <T> UnderlagsperiodeConsumer<T> fraPeriode(final UnderlagsperiodeConsumer<T> consumer) {
            return consumer;
        }

        private <T> UnderlagsperiodeConsumer<T> ignorer() {
            return (u, p) -> {
            };
        }
    };

    @FunctionalInterface
    private interface UnderlagConsumer<T> {
        void accept(final T value, Underlag underlag);
    }

    @FunctionalInterface
    private interface UnderlagsperiodeConsumer<T> {
        void accept(final T value, UnderlagsperiodeBuilder builder);
    }

    private static class Forventning {
        public Predicate<Object> matcher;
        private String message = "";

        public Forventning(final Predicate<Object> matcher) {
            this.matcher = matcher;
        }

        public Forventning as(final String message) {
            this.message = message;
            return this;
        }
    }

    private static class FraOgMedDato {
    }

    private static class TilOgMedDato {
    }

    private static class Feilantall {
    }
}
