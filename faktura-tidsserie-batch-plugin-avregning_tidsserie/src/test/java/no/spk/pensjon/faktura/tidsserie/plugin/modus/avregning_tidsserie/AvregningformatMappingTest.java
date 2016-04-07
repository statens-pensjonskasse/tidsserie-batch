package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer.genererForDato;
import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon.avregningsversjon;
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
import static no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.Fordelingsaarsak.AVKORTET;
import static no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.Fordelingsaarsak.ORDINAER;
import static no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.AntallDagar.antallDagar;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.aarsfaktorRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.aarslengdeRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.aarsverkRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.antallDagarRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.deltidsjustertLoennRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.erMedregningRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.erPermisjonUtanLoenn;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.erUnderMinstegrensaRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.fakturerbareDagsverkGRURegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.fakturerbareDagsverkYSKRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.gruppelivsfaktureringRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.loennstilleggRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.maskineltGrunnlagRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.medregningsRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.minstegrenseRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.oevreLoennsgrenseRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.FalskeReglar.yrkesskadeFaktureringRegel;
import static no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.PremiesatsBuilder.premiesatsBuilder;
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
import no.spk.pensjon.faktura.tidsserie.domain.avregning.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Termintype;
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
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Risikoklasse;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsverkRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.DeltidsjustertLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErMedregningRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErPermisjonUtanLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErUnderMinstegrensaRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.LoennstilleggRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MaskineltGrunnlagRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MedregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Minstegrense;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MinstegrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.OevreLoennsgrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetFaktureringsandel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetGruppelivsfaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetYrkesskadefaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverk;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverkGRURegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverkYSKRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.Fordelingsaarsak;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Beregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

/**
 * Integrasjonstest som verifiserer at {@link Avregningformat} genererer tekstlige verdiar på forventa format.
 * <br>
 * Testen blir satt opp med eit parametrisert sett med forventningar for kvar kolonne og verifiserer ut frå dette
 * at gitt ein bestemt input så blir den tekstlige verdien som kjem ut av mappinga, generert med rett format.
 *
 * @author Tarjei Skorgenes
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(Parameterized.class)
public class AvregningformatMappingTest {
    private static final Premiesats.Builder fakturerbartPensjonsprodukt = premiesats(PEN).produktinfo(new Produktinfo(10)).satser(premiesatsBuilder().arbeidsgiver("10%").medlem("2%").administrasjonsgebyr("0.35%").prosentsatser());
    private static final Premiesats.Builder fakturerbartAFPprodukt = premiesats(AFP).produktinfo(new Produktinfo(40)).satser(premiesatsBuilder().arbeidsgiver("2%").medlem("0").administrasjonsgebyr("0").prosentsatser());
    private static final Premiesats.Builder fakturerbartTIPprodukt = premiesats(TIP).produktinfo(new Produktinfo(94)).satser(premiesatsBuilder().arbeidsgiver("-20%").medlem("0").administrasjonsgebyr("0").prosentsatser());
    private static final Premiesats.Builder fakturerbartGRUprodukt = premiesats(GRU).produktinfo(new Produktinfo(35)).satser(new Satser<>(kroner(535), kroner(0), kroner(35)));
    private static final Premiesats.Builder fakturerbartYSKprodukt = premiesats(YSK).produktinfo(new Produktinfo(71)).satser(new Satser<>(kroner(2535), kroner(0), kroner(35)));

    @Parameterized.Parameters(name = "kolonne={0},type={1}")
    public static List<Object[]> parameters() {
        return Arrays.asList(
                instance(kolonne(1), Observasjonsdato.class, new Observasjonsdato(dato("2015.04.30")), forventa("2015-04-30")),
                instance(kolonne(2), FraOgMedDato.class, dato("2012.02.01"), forventa("2012-02-01")),
                instance(kolonne(3), TilOgMedDato.class, dato("2012.02.29"), forventa("2012-02-29")),
                instance(kolonne(4), Foedselsnummer.class, new Foedselsnummer(foedselsdato(19790806), personnummer(32817)), forventa("19790806")),
                instance(kolonne(5), Foedselsnummer.class, new Foedselsnummer(foedselsdato(19790806), personnummer(32817)), forventa("32817")),
                instance(kolonne(6), StillingsforholdId.class, stillingsforhold(287278692), forventa("287278692")),
                instance(kolonne(7), AvtaleId.class, avtaleId(282762), forventa("282762")),
                instance(kolonne(8), Orgnummer.class, new Orgnummer(123456789L), forventa("123456789")),
                instance(kolonne(9), Ordning.class, Ordning.OPERA, forventa("3035")),
                instance(kolonne(10), Premiestatus.class, Premiestatus.UKJENT, forventa("UKJENT")),
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
                instance(kolonne(23), AarsfaktorRegel.class, aarsfaktorRegel(334d / 365d), forventa("91.5068493")),
                instance(kolonne(23), AarsfaktorRegel.class, aarsfaktorRegel(1d / 365d), forventa("0.2739726")),
                instance(kolonne(24), AarsLengdeRegel.class, aarslengdeRegel(antallDagar(365)), forventa("365")),
                instance(kolonne(25), AarsverkRegel.class, aarsverkRegel(0.5d * 31d / 365d), forventa("4.25")),
                instance(kolonne(25), AarsverkRegel.class, aarsverkRegel(1.0d * 334d / 365d), forventa("91.51")),
                instance(kolonne(26), AntallDagarRegel.class, antallDagarRegel(antallDagar(1)), forventa("1")),
                instance(kolonne(27), DeltidsjustertLoennRegel.class, deltidsjustertLoennRegel(kroner(432_123)), forventa("432123")),
                instance(kolonne(28), LoennstilleggRegel.class, loennstilleggRegel(kroner(0)), forventa("0")),
                instance(kolonne(29), MaskineltGrunnlagRegel.class, maskineltGrunnlagRegel(kroner(1_064_800)), forventa("1064800")),
                instance(kolonne(30), MedregningsRegel.class, medregningsRegel(kroner(10_000)), forventa("10000")),
                instance(kolonne(31), MinstegrenseRegel.class, minstegrenseRegel(new Minstegrense(prosent("99.01%"))), forventa("99.01")),
                instance(kolonne(32), OevreLoennsgrenseRegel.class, oevreLoennsgrenseRegel(new Kroner(9999999999d)), forventa("9999999999")),
                instance(kolonne(33), BegrunnetGruppelivsfaktureringRegel.class, gruppelivsfaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("100%"), ORDINAER)), forventa("100.0000")),
                instance(kolonne(33), BegrunnetGruppelivsfaktureringRegel.class, gruppelivsfaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("0.0001%"), ORDINAER)), forventa("0.0001")),
                instance(kolonne(34), BegrunnetYrkesskadefaktureringRegel.class, yrkesskadeFaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("0%"), ORDINAER)), forventa("0.0000")),
                instance(kolonne(34), BegrunnetYrkesskadefaktureringRegel.class, yrkesskadeFaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("99.999%"), ORDINAER)), forventa("99.9990")),
                instance(kolonne(35), ErMedregningRegel.class, erMedregningRegel(false), forventa("0")),
                instance(kolonne(36), ErPermisjonUtanLoennRegel.class, erPermisjonUtanLoenn(true), forventa("1")),
                instance(kolonne(37), ErUnderMinstegrensaRegel.class, erUnderMinstegrensaRegel(false), forventa("0")),
                instance(kolonne(38), Avtale.class, einPremiesats(eitPensjonsprodukt()), forventa("1")),
                instance(kolonne(39), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("999.0051%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("999.01")),
                instance(kolonne(40), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("999.5551%").administrasjonsgebyr("0%").prosentsatser())), forventa("999.56")),
                instance(kolonne(41), Avtale.class, einPremiesats(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("0%").administrasjonsgebyr("0.354%").prosentsatser())), forventa("0.35")),
                instance(kolonne(42), Avtale.class, einPremiesats(eitPensjonsprodukt().produktinfo(new Produktinfo(10))), forventa("10")),
                instance(kolonne(43), Avtale.class, einPremiesats(eitAFPprodukt()), forventa("1")),
                instance(kolonne(44), Avtale.class, einPremiesats(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("2%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("2.00")),
                instance(kolonne(45), Avtale.class, einPremiesats(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("200%").administrasjonsgebyr("0%").prosentsatser())), forventa("200.00")),
                instance(kolonne(46), Avtale.class, einPremiesats(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("0.00")),
                instance(kolonne(47), Avtale.class, einPremiesats(eitAFPprodukt().produktinfo(new Produktinfo(42))), forventa("42")),
                instance(kolonne(48), Avtale.class, einPremiesats(eitTIPprodukt()), forventa("1")),
                instance(kolonne(49), Avtale.class, einPremiesats(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("108.234%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("108.23")),
                instance(kolonne(50), Avtale.class, einPremiesats(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("-744.499%").administrasjonsgebyr("0%").prosentsatser())), forventa("-744.50")),
                instance(kolonne(51), Avtale.class, einPremiesats(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("0%").medlem("0%").administrasjonsgebyr("0.999%").prosentsatser())), forventa("1.00")),
                instance(kolonne(52), Avtale.class, einPremiesats(eitTIPprodukt().produktinfo(new Produktinfo(95))), forventa("95")),
                instance(kolonne(53), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(new Produktinfo(31))), forventa("0")),
                instance(kolonne(53), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(Produktinfo.GRU_36)), forventa("1")),
                instance(kolonne(54), Avtale.class, einPremiesats(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("99999").medlem("0").administrasjonsgebyr("0").kronesatser())), forventa("99999")),
                instance(kolonne(55), Avtale.class, einPremiesats(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("0").medlem("923").administrasjonsgebyr("0").kronesatser())), forventa("923")),
                instance(kolonne(56), Avtale.class, einPremiesats(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("0").medlem("0").administrasjonsgebyr("17").kronesatser())), forventa("17")),
                instance(kolonne(57), Avtale.class, einPremiesats(eitGRUprodukt().produktinfo(new Produktinfo(39))), forventa("39")),
                instance(kolonne(58), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(Produktinfo.YSK_79)), forventa("0")),
                instance(kolonne(58), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(new Produktinfo(71))), forventa("1")),
                instance(kolonne(59), Avtale.class, einPremiesats(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("4500").medlem("0").administrasjonsgebyr("0").kronesatser())), forventa("4500")),
                instance(kolonne(60), Avtale.class, einPremiesats(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("0").medlem("450").administrasjonsgebyr("0").kronesatser())), forventa("450")),
                instance(kolonne(61), Avtale.class, einPremiesats(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("0").medlem("0").administrasjonsgebyr("-45").kronesatser())), forventa("-45")),
                instance(kolonne(62), Avtale.class, einPremiesats(eitYSKprodukt().produktinfo(new Produktinfo(70))), forventa("70")),
                instance(kolonne(63), Avtale.class, einAvtale(eitYSKprodukt()).risikoklasse(of(new Risikoklasse("1,5"))).bygg(), forventa("1,5")),
                instance(kolonne(64), UUID.class, null, matches("^\\w{8}-\\w+{4}-\\w+{4}-\\w{4}-\\w{12}$")),
                instance(kolonne(65), Feilantall.class, null, forventa("0")),
                instance(kolonne(66), ArbeidsgiverId.class, new ArbeidsgiverId(100_000L), forventa("100000")),
                instance(kolonne(67), Tidsserienummer.class, genererForDato(dato("2016.01.07")), forventa("20160107")),
                instance(kolonne(68), Termintype.class, Termintype.UKJENT, forventa("UKJ")),
                instance(kolonne(69), Medlemslinjenummer.class, Medlemslinjenummer.linjenummer(18), forventa("18")),
                instance(kolonne(70), Premiekategori.class, Premiekategori.HENDELSESBASERT, forventa("LOP")),
                instance(kolonne(71), Avregningsversjon.class, avregningsversjon(58), forventa("58")),
                instance(kolonne(72), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("10.01%").medlem("2.55%").administrasjonsgebyr("0.35%").prosentsatser())), forventa("2.55")),
                instance(kolonne(73), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("10.01%").medlem("2.55%").administrasjonsgebyr("0.35%").prosentsatser())), forventa("10.01")),
                instance(kolonne(74), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitPensjonsprodukt().satser(premiesatsBuilder().arbeidsgiver("10.01%").medlem("2.55%").administrasjonsgebyr("0.35%").prosentsatser())), forventa("0.35")),

                instance(kolonne(75), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("4%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("0.00")),
                instance(kolonne(76), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("4%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("4.00")),
                instance(kolonne(77), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitAFPprodukt().satser(premiesatsBuilder().arbeidsgiver("4%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("0.00")),

                instance(kolonne(78), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("120.99%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("0.00")),
                instance(kolonne(79), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("120.99%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("120.99")),
                instance(kolonne(80), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitTIPprodukt().satser(premiesatsBuilder().arbeidsgiver("120.99%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())), forventa("0.00")),

                instance(kolonne(81), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("1000").medlem("100").administrasjonsgebyr("35").kronesatser())), forventa("0.00")),
                instance(kolonne(82), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("1000").medlem("100").administrasjonsgebyr("35").kronesatser())), forventa("0.00")),
                instance(kolonne(83), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitGRUprodukt().satser(premiesatsBuilder().arbeidsgiver("1000").medlem("100").administrasjonsgebyr("35").kronesatser())), forventa("0.00")),

                instance(kolonne(84), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("2000").medlem("200").administrasjonsgebyr("36").kronesatser())), forventa("0.00")),
                instance(kolonne(85), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("2000").medlem("200").administrasjonsgebyr("36").kronesatser())), forventa("0.00")),
                instance(kolonne(86), GrunnlagOgPremiesats.class, grunnlag(kroner(100)).og(eitYSKprodukt().satser(premiesatsBuilder().arbeidsgiver("2000").medlem("200").administrasjonsgebyr("36").kronesatser())), forventa("0.00")),

                instance(kolonne(87), String.class, "", forventa("")),
                instance(kolonne(88), FakturerbareDagsverkGRURegel.class, fakturerbareDagsverkGRURegel(new FakturerbareDagsverk(5)), forventa("5.00000")),
                instance(kolonne(89), FakturerbareDagsverkYSKRegel.class, fakturerbareDagsverkYSKRegel(new FakturerbareDagsverk(5)), forventa("5.00000")),
                instance(kolonne(90), BegrunnetGruppelivsfaktureringRegel.class, gruppelivsfaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("99.9999%"), ORDINAER)), forventa("ORD")),
                instance(kolonne(91), BegrunnetYrkesskadefaktureringRegel.class, yrkesskadeFaktureringRegel(new BegrunnetFaktureringsandel(stillingsforhold(1L), prosent("9.9999%"), AVKORTET)), forventa("AVK")),

                instance(
                        kolonne(73
                        ),
                        GrunnlagOgPremiesats.class,
                        grunnlag(kroner(1_060_440))
                                .og(eitPensjonsprodukt().kopi().satser(premiesatsBuilder().arbeidsgiver("100%").medlem("0%").administrasjonsgebyr("0%").prosentsatser())),
                        forventa("1060440.00")
                )
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

    private final Avregningformat format = new Avregningformat();

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
        final Underlag underlag = new Underlag(Stream.of(builder.bygg()));
        underlag.annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.12.31")));
        return underlag;
    }

    private static UnderlagsperiodeBuilder eiPeriode() {
        final StillingsforholdId stillingsforhold = stillingsforhold(1L);
        final UnderlagsperiodeBuilder builder = new UnderlagsperiodeBuilder()
                .fraOgMed(dato("2012.01.01"))
                .tilOgMed(dato("2012.12.31"))
                .med(avregningsversjon(21))
                .med(new Foedselsnummer(foedselsdato(19800101), personnummer(1)))
                .med(stillingsforhold)
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
                .med(AktiveStillingar.class, () -> Stream.of(new AktiveStillingar.AktivStilling(stillingsforhold, empty(), empty())))
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
                .med(Avtale.class, avtale(avtaleId(210_400)).bygg());
        new AvregningsRegelsett().reglar().forEach(p -> p.annoter(builder));
        return builder;
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

    
    @SuppressWarnings({"serial"})
    private static Map<Object, Object> populators = new HashMap<Object, Object>() {
        {
            put(Observasjonsdato.class, fraUnderlag((Observasjonsdato value, Underlag underlag) -> underlag.annoter(Observasjonsdato.class, value)));
            put(FraOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.fraOgMed(value)));
            put(TilOgMedDato.class, fraPeriode((LocalDate value, UnderlagsperiodeBuilder b) -> b.tilOgMed(value)));
            put(Feilantall.class, ignorer());
            put(Tidsserienummer.class, fraUnderlag((Tidsserienummer value, Underlag underlag) -> underlag.annoter(Tidsserienummer.class, value)));
            put(GrunnlagOgPremiesats.class, fraPeriode(GrunnlagOgPremiesats::annoter));
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

    private static class GrunnlagOgPremiesats {
        private Kroner beloep;
        private Premiesats premiesats;

        public GrunnlagOgPremiesats grunnlag(final Kroner beloep) {
            this.beloep = beloep;
            return this;
        }

        public GrunnlagOgPremiesats og(final Premiesats.Builder satser) {
            this.premiesats = satser.kopi().bygg();
            return this;
        }

        public void annoter(final UnderlagsperiodeBuilder builder) {
            builder
                    .med(
                            MaskineltGrunnlagRegel.class,
                            new MaskineltGrunnlagRegel() {
                                @Override
                                public Kroner beregn(final Beregningsperiode<?> periode) {
                                    return beloep;
                                }
                            }
                    )
                    .med(
                            Avtale.class,
                            avtale(avtaleId(200_000L))
                                    .addPremiesats(premiesats)
                                    .bygg()
                    )
            ;
        }
    }

    private static GrunnlagOgPremiesats grunnlag(final Kroner beloep) {
        return new GrunnlagOgPremiesats().grunnlag(beloep);
    }
}
