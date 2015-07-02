package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode.PERMISJON_UTAN_LOENN;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.DeltidsjustertLoenn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Fastetillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Funksjonstillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Grunnbeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.LoennstrinnBeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
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
 *
 * @author Tarjei Skorgenes
 */
public class Datavarehusformat implements CSVFormat {
    //
    private final ThreadLocal<Map<Integer, NumberFormat>> desimalformat = ThreadLocal.withInitial(HashMap::new);

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
                "premiekategori",
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
                "termintype"
        );
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        final ErrorDetector detector = new ErrorDetector();
        final Optional<Stillingsprosent> deltid = p.valgfriAnnotasjonFor(Stillingsprosent.class);

        final Stream.Builder<Object> builder = Stream
                .builder()
                .add(dato(observasjonsunderlag.annotasjonFor(Observasjonsdato.class).dato()))
                .add(dato(p.fraOgMed()))
                .add(dato(p.tilOgMed().get()))
                .add(kode(p.annotasjonFor(Foedselsnummer.class).toString()))
                .add(heiltall(p.annotasjonFor(StillingsforholdId.class).id()))
                .add(heiltall(p.annotasjonFor(AvtaleId.class).id()));

        detector.utfoer(builder, p, up -> kode(organisasjonsnummer()))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Ordning.class).map(Ordning::kode).map(Object::toString)))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)))
                .utfoer(builder, p, up -> kode(premiekategori()))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Aksjonskode.class).map(Aksjonskode::kode)))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Stillingskode.class).map(Stillingskode::getKode)))
                .utfoer(builder, p, up -> prosent(deltid.map(Stillingsprosent::prosent), 3))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Loennstrinn.class).map(Loennstrinn::trinn).map(Object::toString)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(LoennstrinnBeloep.class).map(LoennstrinnBeloep::beloep)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(DeltidsjustertLoenn.class).map(DeltidsjustertLoenn::beloep)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Fastetillegg.class).map(Fastetillegg::beloep)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Variabletillegg.class).map(Variabletillegg::beloep)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Funksjonstillegg.class).map(Funksjonstillegg::beloep)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Medregning.class).map(Medregning::beloep)))
                .utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Medregningskode.class).map(Medregningskode::kode)))
                .utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Grunnbeloep.class).map(Grunnbeloep::beloep)))
                .utfoer(builder, p, up -> prosent(up.beregn(AarsfaktorRegel.class).tilProsent(), 8))
                .utfoer(builder, p, up -> heiltall(up.beregn(AarsLengdeRegel.class).verdi()))
                .utfoer(builder, p, up -> prosent(up.beregn(AarsverkRegel.class).tilProsent(), 2))
                .utfoer(builder, p, up -> heiltall(up.beregn(AntallDagarRegel.class).verdi()))
                .utfoer(builder, p, up -> beloep(up.beregn(DeltidsjustertLoennRegel.class)))
                .utfoer(builder, p, up -> beloep(up.beregn(LoennstilleggRegel.class)))
                .utfoer(builder, p, up -> beloep(up.beregn(MaskineltGrunnlagRegel.class)))
                .utfoer(builder, p, up -> beloep(up.beregn(MedregningsRegel.class)))
                .utfoer(builder, p, up -> prosent(up.beregn(MinstegrenseRegel.class).grense(), 2))
                .utfoer(builder, p, up -> beloep(up.beregn(OevreLoennsgrenseRegel.class)))
                .utfoer(builder, p, up -> prosent(up.beregn(GruppelivsfaktureringRegel.class).andel(), 4))
                .utfoer(builder, p, up -> prosent(up.beregn(YrkesskadefaktureringRegel.class).andel(), 4))
                .utfoer(builder, p, up -> flagg(up.valgfriAnnotasjonFor(Medregning.class).isPresent()))
                .utfoer(builder, p, up -> flagg(up.valgfriAnnotasjonFor(Aksjonskode.class).filter(kode -> kode.equals(PERMISJON_UTAN_LOENN)).isPresent()))
                .utfoer(builder, p, up -> flagg(deltid.map(d -> up.beregn(MinstegrenseRegel.class).erUnderMinstegrensa(d)).orElse(false)))
                .utfoer(builder, p, up -> premiesatserFor(up, Produkt.PEN), 4)
                .utfoer(builder, p, up -> premiesatserFor(up, Produkt.AFP), 4)
                .utfoer(builder, p, up -> premiesatserFor(up, Produkt.TIP), 4)
                .utfoer(builder, p, up -> premiesatserFor(up, Produkt.GRU), 4)
                .utfoer(builder, p, up -> premiesatserFor(up, Produkt.YSK), 4)
                .utfoer(builder, p, up -> kode(risikoklasse()))
                .utfoer(builder, p, up -> up.id().toString())
        ;

        // Sidan kolonna for antall feil ikkje er siste kolonne i formatet, må det leggast inn ein
        // midlertidig verdi her for å reservere kolonneposisjonen og sikre at seinare kolonner ikkje blir forskyvd
        // ei kolonne fram
        final Object placeholder = new Object();
        builder.add(placeholder);

        detector.utfoer(builder, p, up -> arbeidsgivernummer())
                .utfoer(builder, p, up -> tidsserienummer())
                .utfoer(builder, p, up -> termintype())
        ;

        // Må populere inn antall feil til slutt for å sikre at eventuelle feil som inntreffe etter at denne kolonna
        // blir lagt til builderen, blir med i tellinga
        return builder.build().map(o -> o == placeholder ? heiltall(detector.antallFeil) : o);
    }

    private String termintype() {
        return kode("");
    }

    private String tidsserienummer() {
        return kode("");
    }

    private String arbeidsgivernummer() {
        return kode("");
    }

    private Optional<String> organisasjonsnummer() {
        return empty();
    }

    private Optional<String> premiekategori() {
        return empty();
    }

    private Optional<String> risikoklasse() {
        return of("2,5");
    }

    private String dato(final LocalDate verdi) {
        return verdi.toString();
    }

    private String heiltall(final int verdi) {
        return Integer.toString(verdi);
    }

    private String heiltall(final long verdi) {
        return Long.toString(verdi);
    }

    private String flagg(final boolean value) {
        return value ? "1" : "0";
    }

    private String prosent(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosent(p, antallDesimaler)).orElse("");
    }

    private String prosent(final Prosent verdi, final int antallDesimaler) {
        return desimalFormat(antallDesimaler).format(verdi.toDouble() * 100d);
    }

    private String kode(final String verdi) {
        return verdi;
    }

    private String kode(final Optional<String> verdi) {
        return verdi.orElse("");
    }

    private String beloep(final Optional<Kroner> verdi) {
        return verdi.map(Kroner::verdi).map(Object::toString).orElse("");
    }

    private String beloep(final Kroner beloep) {
        return Long.toString(beloep.verdi());
    }

    private Stream<String> premiesatserFor(final Underlagsperiode up, final Produkt produkt) {
        return Stream.of(
                flagg(false),
                beloep(Optional.<Kroner>empty()),
                beloep(Optional.<Kroner>empty()),
                beloep(Optional.<Kroner>empty()),
                kode(Optional.<String>empty())
        );
    }

    private NumberFormat desimalFormat(final int antallDesimaler) {
        return desimalformat.get().computeIfAbsent(antallDesimaler, antall -> {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(antall);
            format.setMinimumFractionDigits(antall);
            return format;
        });
    }

    private static class ErrorDetector {
        int antallFeil;

        ErrorDetector utfoer(final Consumer<Object> builder, final Underlagsperiode p, final Function<Underlagsperiode, String> function) {
            try {
                builder.accept(function.apply(p));
            } catch (final Exception e) {
                antallFeil++;
                builder.accept("");
            }
            return this;
        }

        ErrorDetector utfoer(final Consumer<Object> builder, final Underlagsperiode p, final Function<Underlagsperiode, Stream<String>> function, final int fieldCount) {
            try {
                function.apply(p)
                        .forEach(builder);
            } catch (final Exception e) {
                antallFeil++;

                range(0, fieldCount)
                        .mapToObj(i -> "")
                        .forEach(builder);
            }
            return this;
        }
    }
}
