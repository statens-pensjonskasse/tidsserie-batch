package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato.foedselsdato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn.loennstrinn;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer.personnummer;
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
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.LoennstrinnBeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medlemsavtalar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;

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

    @Parameterized.Parameters(name = "kolonne={0},type={1}")
    public static List<Object[]> parameters() {
        return Arrays.<Object[]>asList(
                instance(kolonne(1), Observasjonsdato.class, new Observasjonsdato(dato("2015.04.30")), forventa("2015-04-30")),
                instance(kolonne(2), FraOgMedDato.class, dato("2012.02.01"), forventa("2012-02-01")),
                instance(kolonne(3), TilOgMedDato.class, dato("2012.02.29"), forventa("2012-02-29")),
                instance(kolonne(4), Foedselsnummer.class, new Foedselsnummer(foedselsdato(dato("1979.08.06")), personnummer(32817)), forventa("1979080632817")),
                instance(kolonne(5), StillingsforholdId.class, stillingsforhold(287278692), forventa("287278692")),
                instance(kolonne(6), AvtaleId.class, avtaleId(282762), forventa("282762")),
                instance(kolonne(7), Orgnummer.class, new Orgnummer(123456789L), forventa("123456789")),
                instance(kolonne(8), Ordning.class, Ordning.OPERA, forventa("3035")),
                instance(kolonne(9), Premiestatus.class, Premiestatus.UKJENT, forventa("UKJENT")),
                instance(kolonne(10), String.class, "", forventa("")), // Premiekategori, foreløpig ikkje en del av tidsserien
                instance(kolonne(11), Aksjonskode.class, Aksjonskode.ENDRINGSMELDING, forventa("021")),
                instance(kolonne(12), Stillingskode.class, Stillingskode.K_STIL_APO_GENERALSEKRETER, forventa("14")),
                instance(kolonne(13), Stillingsprosent.class, fulltid(), forventa("100.000")),
                instance(kolonne(14), Loennstrinn.class, loennstrinn(100), forventa("100")),
                instance(kolonne(15), LoennstrinnBeloep.class, new LoennstrinnBeloep(kroner(1_050_123)), forventa("1050123")),
                instance(kolonne(16), DeltidsjustertLoenn.class, new DeltidsjustertLoenn(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(17), Fastetillegg.class, new Fastetillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(18), Variabletillegg.class, new Variabletillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(19), Funksjonstillegg.class, new Funksjonstillegg(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(20), Medregning.class, new Medregning(kroner(45_000_200)), forventa("45000200")),
                instance(kolonne(21), Medregningskode.class, Medregningskode.TILLEGG_ANNEN_ARBGIV, forventa("14")),
                instance(kolonne(22), Grunnbeloep.class, new Grunnbeloep(kroner(120_987)), forventa("120987")),
                instance(kolonne(63), String.class, "", forventa("2,5")),
                instance(kolonne(64), UUID.class, null, matches("^\\w{8}-\\w+{4}-\\w+{4}-\\w{4}-\\w{12}$")),
                instance(kolonne(65), Feilantall.class, null, forventa("0")),
                instance(kolonne(66), ArbeidsgiverId.class, new ArbeidsgiverId(100_000L), forventa("100000")),

                // Premiesatsar, foreløpig hardkoda i formatet i påvente av implementasjon av oppslag

                // PEN
                instance(kolonne(38), String.class, "", forventa("0")),
                instance(kolonne(39), String.class, "", forventa("")),
                instance(kolonne(40), String.class, "", forventa("")),
                instance(kolonne(41), String.class, "", forventa("")),
                instance(kolonne(42), String.class, "", forventa("")),
                // AFP
                instance(kolonne(43), String.class, "", forventa("0")),
                instance(kolonne(44), String.class, "", forventa("")),
                instance(kolonne(45), String.class, "", forventa("")),
                instance(kolonne(46), String.class, "", forventa("")),
                instance(kolonne(47), String.class, "", forventa("")),
                // TIP
                instance(kolonne(48), String.class, "", forventa("0")),
                instance(kolonne(49), String.class, "", forventa("")),
                instance(kolonne(50), String.class, "", forventa("")),
                instance(kolonne(51), String.class, "", forventa("")),
                instance(kolonne(52), String.class, "", forventa("")),
                // YSK
                instance(kolonne(53), String.class, "", forventa("0")),
                instance(kolonne(54), String.class, "", forventa("")),
                instance(kolonne(55), String.class, "", forventa("")),
                instance(kolonne(56), String.class, "", forventa("")),
                instance(kolonne(57), String.class, "", forventa("")),
                // GRU
                instance(kolonne(58), String.class, "", forventa("0")),
                instance(kolonne(59), String.class, "", forventa("")),
                instance(kolonne(60), String.class, "", forventa("")),
                instance(kolonne(61), String.class, "", forventa("")),
                instance(kolonne(62), String.class, "", forventa(""))

                // TODO: Reglar, dei blir meir komplekse oppsettsmessig sidan dei treng langt meir tilstand pr regel
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
            return populer(builder, c);
        } else if (consumer instanceof UnderlagsperiodeConsumer) {
            final UnderlagsperiodeConsumer c = (UnderlagsperiodeConsumer) consumer;
            return populer(builder, c);
        }
        return null;
    }

    private Underlag populer(UnderlagsperiodeBuilder builder, UnderlagsperiodeConsumer c) {
        c.accept(value, builder);
        return eitUnderlag(builder);
    }

    private Underlag populer(UnderlagsperiodeBuilder builder, UnderlagConsumer c) {
        final Underlag underlag = eitUnderlag(builder);
        c.accept(value, underlag);
        return underlag;
    }

    private Underlag eitUnderlag(UnderlagsperiodeBuilder builder) {
        final Underlag underlag = new Underlag(Stream.of(builder.bygg()));
        underlag.annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.12.31")));
        return underlag;
    }

    private static UnderlagsperiodeBuilder eiPeriode() {
        return new UnderlagsperiodeBuilder()
                .fraOgMed(dato("2012.01.01"))
                .tilOgMed(dato("2012.12.31"))
                .med(new Foedselsnummer(foedselsdato(dato("1980.01.01")), personnummer(1)))
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
                .reglar(new AvregningsRegelsett())
                ;
    }

    private static int kolonne(int nr) {
        return nr;
    }

    private static Forventning forventa(final String verdi) {
        return new Forventning(v -> v.equals(verdi)).as("is equal to " + verdi);
    }

    private static Forventning matches(final String pattern) {
        return new Forventning(v -> v instanceof String && ((String) v).matches(pattern)).as("regular expression " + pattern);
    }

    private static Object[] instance(final Object... args) {
        if (args.length == 4) {
            return Stream.concat(
                    asList(args).stream(),
                    Stream.of(eiPeriode())
            ).toArray();
        }
        return args;
    }

    private static Map<Object, Object> populators = new HashMap<Object, Object>() {
        {
            put(Observasjonsdato.class, fraUnderlag((Observasjonsdato value, Underlag underlag) -> underlag.annoter(Observasjonsdato.class, value)));
            put(FraOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.fraOgMed(value)));
            put(TilOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.tilOgMed(value)));
            put(Feilantall.class, ignorer());
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
