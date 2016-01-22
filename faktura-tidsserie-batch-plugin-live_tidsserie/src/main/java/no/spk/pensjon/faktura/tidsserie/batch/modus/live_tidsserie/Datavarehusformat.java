package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode.PERMISJON_UTAN_LOENN;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.beloep;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.flagg;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.heiltall;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.kode;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode;
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
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medlemslinjenummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsverkRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.DeltidsjustertLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.GruppelivsfaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.LoennstilleggRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MaskineltGrunnlagRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MedregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MinstegrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.OevreLoennsgrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.TermintypeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.YrkesskadefaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link Datavarehusformat} representerer kontrakta og mapping-strategien for konvertering av {@link Underlagsperiode}r til
 * CSV-formaterte rader for innmating i DVH og Qlikview for live-tidsserien til FFF.
 * <br>
 * Merk at ei kvar utad-synlig endring på formatet må avklarast mot Team Polaris for å verifisere om eller korleis det
 * påvirkar EDW og DVH-modelleringa + ETL-jobbane i DVH og Qlikview.
 * <br>
 * Nye kolonner skal alltid leggast til etter siste eksisterande kolonne og dokumenterast i kontrakta på wiki.
 * <br>
 * Formatendringar på eksisterande kolonner bør ein unngå i så stor grad som mulig. Fortrinnsvis bør ein vurdere
 * å beholde gamal kolonne på gamalt format og legge til ny kolonne for nytt format.
 * <br>
 * Merk at foreløpig handhevar ikkje formatet kontrakta fullt ut med tanke på lengdeavgrensing av felt som potensielt
 * sett kan ha verdiar som blir lenger enn det kontrakta tillater.
 * <br>
 * Kontrakta for utvekslingsformatet ligg tilgjengelig under
 * <a href="http://wiki/confluence/display/dok/faktura-tidsserie-batch+-+CSV-format+for+DVH+og+Qlikview">faktura-tidsserie-batch - CSV-format for DVH og Qlikview</a>
 *
 * @author Tarjei Skorgenes
 */
public class Datavarehusformat implements CSVFormat {
    private final Desimaltallformatering desimaltall = new Desimaltallformatering();

    private final Premiesatskolonner premiesatskolonner = new Premiesatskolonner();

    @Override
    public Stream<String> kolonnenavn() {
        return Stream.of(
                "observasjonsdato",
                "fraOgMedDato",
                "tilOgMedDato",
                "medlem",
                "stillingsforhold",
                "avtale",
                "organisasjonsnummer",
                "ordning",
                "premiestatus",
                "aksjonskode",
                "stillingskode",
                "stillingsprosent",
                "loennstrinn",
                "loennstrinnBeloep",
                "deltidsjustertLoenn",
                "fasteTillegg",
                "variableTillegg",
                "funksjonsTillegg",
                "medregning",
                "medregningskode",
                "grunnbeloep",
                "regel_aarsfaktor",
                "regel_aarslengde",
                "regel_aarsverk",
                "regel_antalldager",
                "regel_deltidsjustertloenn",
                "regel_loennstillegg",
                "regel_pensjonsgivende_loenn",
                "regel_medregning",
                "regel_minstegrense",
                "regel_oevreLoennsgrense",
                "regel_gruppelivsandel",
                "regel_yrkesskadeandel",
                "regel_erMedregning",
                "regel_erPermisjonUtenLoenn",
                "regel_erUnderMinstegrensen",
                "produkt_PEN",
                "produkt_PEN_satsArbeidsgiver",
                "produkt_PEN_satsMedlem",
                "produkt_PEN_satsAdministrasjonsgebyr",
                "produkt_PEN_produktinfo",
                "produkt_AFP",
                "produkt_AFP_satsArbeidsgiver",
                "produkt_AFP_satsMedlem",
                "produkt_AFP_satsAdministrasjonsgebyr",
                "produkt_AFP_produktinfo",
                "produkt_TIP",
                "produkt_TIP_satsArbeidsgiver",
                "produkt_TIP_satsMedlem",
                "produkt_TIP_satsAdministrasjonsgebyr",
                "produkt_TIP_produktinfo",
                "produkt_GRU",
                "produkt_GRU_satsArbeidsgiver",
                "produkt_GRU_satsMedlem",
                "produkt_GRU_satsAdministrasjonsgebyr",
                "produkt_GRU_produktinfo",
                "produkt_YSK",
                "produkt_YSK_satsArbeidsgiver",
                "produkt_YSK_satsMedlem",
                "produkt_YSK_satsAdministrasjonsgebyr",
                "produkt_YSK_produktinfo",
                "produkt_YSK_risikoklasse",
                "uuid",
                "antallFeil",
                "arbeidsgivernummer",
                "tidsserienummer",
                "termintype",
                "linjenummer_historikk",
                "premiekategori"
        );
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        final ErrorDetector detector = new ErrorDetector();
        final Optional<Stillingsprosent> deltid = p.valgfriAnnotasjonFor(Stillingsprosent.class);

        final Stream.Builder<Object> builder = Stream
                .builder()
                .add(Kolonnetyper.dato(observasjonsunderlag.annotasjonFor(Observasjonsdato.class).dato()))
                .add(Kolonnetyper.dato(p.fraOgMed()))
                .add(Kolonnetyper.dato(p.tilOgMed().get()))
                .add(Kolonnetyper.kode(p.annotasjonFor(Foedselsnummer.class).toString()))
                .add(Kolonnetyper.heiltall(p.annotasjonFor(StillingsforholdId.class).id()))
                .add(Kolonnetyper.heiltall(p.annotasjonFor(AvtaleId.class).id()));

        detector.utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Orgnummer.class).map(Orgnummer::id)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Ordning.class).map(Ordning::kode)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Aksjonskode.class).map(Aksjonskode::kode)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Stillingskode.class).map(Stillingskode::getKode)))
                .utfoer(builder, p, up -> prosent(deltid.map(Stillingsprosent::prosent), 3))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Loennstrinn.class).map(Loennstrinn::trinn)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(LoennstrinnBeloep.class).map(LoennstrinnBeloep::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(DeltidsjustertLoenn.class).map(DeltidsjustertLoenn::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(Fastetillegg.class).map(Fastetillegg::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(Variabletillegg.class).map(Variabletillegg::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(Funksjonstillegg.class).map(Funksjonstillegg::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(Medregning.class).map(Medregning::beloep)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Medregningskode.class).map(Medregningskode::kode)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.valgfriAnnotasjonFor(Grunnbeloep.class).map(Grunnbeloep::beloep)))
                .utfoer(builder, p, up -> prosent(up.beregn(AarsfaktorRegel.class).tilProsent(), 8))
                .utfoer(builder, p, up -> Kolonnetyper.heiltall(up.beregn(AarsLengdeRegel.class).verdi()))
                .utfoer(builder, p, up -> prosent(up.beregn(AarsverkRegel.class).tilProsent(), 2))
                .utfoer(builder, p, up -> Kolonnetyper.heiltall(up.beregn(AntallDagarRegel.class).verdi()))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.beregn(DeltidsjustertLoennRegel.class)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.beregn(LoennstilleggRegel.class)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.beregn(MaskineltGrunnlagRegel.class)))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.beregn(MedregningsRegel.class)))
                .utfoer(builder, p, up -> prosent(up.beregn(MinstegrenseRegel.class).grense(), 2))
                .utfoer(builder, p, up -> Kolonnetyper.beloep(up.beregn(OevreLoennsgrenseRegel.class)))
                .utfoer(builder, p, up -> prosent(up.beregn(GruppelivsfaktureringRegel.class).andel(), 4))
                .utfoer(builder, p, up -> prosent(up.beregn(YrkesskadefaktureringRegel.class).andel(), 4))
                .utfoer(builder, p, up -> Kolonnetyper.flagg(up.valgfriAnnotasjonFor(Medregning.class).isPresent()))
                .utfoer(builder, p, up -> Kolonnetyper.flagg(up.valgfriAnnotasjonFor(Aksjonskode.class).filter(kode -> kode.equals(PERMISJON_UTAN_LOENN)).isPresent()))
                .utfoer(builder, p, up -> Kolonnetyper.flagg(deltid.map(d -> up.beregn(MinstegrenseRegel.class).erUnderMinstegrensa(d)).orElse(false)))

                .multiple(builder, p, premiesatsar(p, Produkt.PEN))
                .multiple(builder, p, premiesatsar(p, Produkt.AFP))
                .multiple(builder, p, premiesatsar(p, Produkt.TIP))
                .multiple(builder, p, premiesatsar(p, Produkt.GRU))
                .multiple(builder, p, premiesatsar(p, Produkt.YSK))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Avtale.class).flatMap(Avtale::risikoklasse)))
                .utfoer(builder, p, up -> up.id().toString())
        ;

        // Sidan kolonna for antall feil ikkje er siste kolonne i formatet, må det leggast inn ein
        // midlertidig verdi her for å reservere kolonneposisjonen og sikre at seinare kolonner ikkje blir forskyvd
        // ei kolonne fram
        final Object placeholder = new Object();
        builder.add(placeholder);

        detector.utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(ArbeidsgiverId.class).map(ArbeidsgiverId::id)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(observasjonsunderlag.valgfriAnnotasjonFor(Tidsserienummer.class)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.beregn(TermintypeRegel.class).kode()))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Medlemslinjenummer.class)))
                .utfoer(builder, p, up -> Kolonnetyper.kode(up.valgfriAnnotasjonFor(Premiekategori.class).map(Premiekategori::kode)))
        ;

        // Må populere inn antall feil til slutt for å sikre at eventuelle feil som inntreffe etter at denne kolonna
        // blir lagt til builderen, blir med i tellinga
        return builder.build().map(o -> o == placeholder ? Kolonnetyper.heiltall(detector.antallFeil) : o);
    }

    private Stream<Function<Underlagsperiode, String>> premiesatsar(final Underlagsperiode p, final Produkt produkt) {
        return premiesatskolonner.forProdukt(p, produkt);
    }

    private String prosent(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosent(p, antallDesimaler)).orElse("");
    }

    private String prosent(final Prosent verdi, final int antallDesimaler) {
        return desimaltall.formater(verdi.toDouble() * 100d, antallDesimaler);
    }
}
