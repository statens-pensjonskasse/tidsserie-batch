package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode.PERMISJON_UTAN_LOENN;

import java.text.NumberFormat;
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

public class Datavarehusformat implements CSVFormat {
    private final Map<Integer, NumberFormat> desimalformat = new HashMap<>();

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
                "antallFeil"
        );
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        final ErrorDetector detector = new ErrorDetector();
        final Optional<Stillingsprosent> deltid = p.valgfriAnnotasjonFor(Stillingsprosent.class);
        final StillingsforholdId stilling = observasjonsunderlag.annotasjonFor(StillingsforholdId.class);

        final Stream.Builder<Object> builder = Stream
                .builder()
                .add(observasjonsunderlag.annotasjonFor(Observasjonsdato.class).dato())
                .add(p.fraOgMed())
                .add(p.tilOgMed().get())
                .add(p.annotasjonFor(Foedselsnummer.class))
                .add(stilling.id())
                .add(p.annotasjonFor(AvtaleId.class).id());

        detector.utfoer(builder, p, up -> kode(empty()));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Ordning.class).map(Ordning::kode).map(Object::toString)));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)));
        detector.utfoer(builder, p, up -> kode(empty()));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Aksjonskode.class).map(Aksjonskode::kode)));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Stillingskode.class).map(Stillingskode::getKode)));
        detector.utfoer(builder, p, up -> prosent(deltid.map(Stillingsprosent::prosent), 3));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Loennstrinn.class).map(Loennstrinn::trinn).map(Object::toString)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(LoennstrinnBeloep.class).map(LoennstrinnBeloep::beloep)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(DeltidsjustertLoenn.class).map(DeltidsjustertLoenn::beloep)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Fastetillegg.class).map(Fastetillegg::beloep)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Variabletillegg.class).map(Variabletillegg::beloep)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Funksjonstillegg.class).map(Funksjonstillegg::beloep)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Medregning.class).map(Medregning::beloep)));
        detector.utfoer(builder, p, up -> kode(up.valgfriAnnotasjonFor(Medregningskode.class).map(Medregningskode::kode)));
        detector.utfoer(builder, p, up -> beloep(up.valgfriAnnotasjonFor(Grunnbeloep.class).map(Grunnbeloep::beloep)));
        detector.utfoer(builder, p, up -> prosent(up.beregn(AarsfaktorRegel.class).tilProsent(), 2));
        detector.utfoer(builder, p, up -> up.beregn(AarsLengdeRegel.class).verdi());
        detector.utfoer(builder, p, up -> prosent(up.beregn(AarsverkRegel.class).tilProsent(), 2));
        detector.utfoer(builder, p, up -> up.beregn(AntallDagarRegel.class).verdi());
        detector.utfoer(builder, p, up -> beloep(up.beregn(DeltidsjustertLoennRegel.class)));
        detector.utfoer(builder, p, up -> beloep(up.beregn(LoennstilleggRegel.class)));
        detector.utfoer(builder, p, up -> beloep(up.beregn(MaskineltGrunnlagRegel.class)));
        detector.utfoer(builder, p, up -> beloep(up.beregn(MedregningsRegel.class)));
        detector.utfoer(builder, p, up -> prosent(up.beregn(MinstegrenseRegel.class).grense(), 2));
        detector.utfoer(builder, p, up -> beloep(up.beregn(OevreLoennsgrenseRegel.class)));
        detector.utfoer(builder, p, up -> prosent(up.beregn(GruppelivsfaktureringRegel.class).andel(), 4));
        detector.utfoer(builder, p, up -> prosent(up.beregn(YrkesskadefaktureringRegel.class).andel(), 4));
        detector.utfoer(builder, p, up -> flagg(up.valgfriAnnotasjonFor(Medregning.class).isPresent()));
        detector.utfoer(builder, p, up -> flagg(up.valgfriAnnotasjonFor(Aksjonskode.class).filter(kode -> kode.equals(PERMISJON_UTAN_LOENN)).isPresent()));
        detector.utfoer(builder, p, up -> flagg(deltid.map(d -> up.beregn(MinstegrenseRegel.class).erUnderMinstegrensa(d)).orElse(false)));
        detector.utfoer(builder, p, up -> premiesatserFor(up, Produkt.PEN), 4);
        detector.utfoer(builder, p, up -> premiesatserFor(up, Produkt.AFP), 4);
        detector.utfoer(builder, p, up -> premiesatserFor(up, Produkt.TIP), 4);
        detector.utfoer(builder, p, up -> premiesatserFor(up, Produkt.GRU), 4);
        detector.utfoer(builder, p, up -> premiesatserFor(up, Produkt.YSK), 4);
        detector.utfoer(builder, p, up -> kode(of("2,5")));

        detector.utfoer(builder, p, up -> up.id().toString());

        return builder
                .add(detector.antallFeil)
                .build();
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
        return desimalformat.computeIfAbsent(antallDesimaler, antall -> {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(antall);
            format.setMinimumFractionDigits(antall);
            return format;
        });
    }

    static class ErrorDetector {
        int antallFeil;

        void utfoer(final Consumer<Object> builder, final Underlagsperiode p, final Function<Underlagsperiode, Object> function) {
            try {
                builder.accept(function.apply(p));
            } catch (final Exception e) {
                antallFeil++;
                builder.accept("");
            }
        }

        void utfoer(final Consumer<Object> builder, final Underlagsperiode p, final Function<Underlagsperiode, Stream<String>> function, final int fieldCount) {
            try {
                function.apply(p)
                        .forEach(builder);
            } catch (final Exception e) {
                antallFeil++;

                range(0, fieldCount)
                        .mapToObj(i -> "")
                        .forEach(builder);
            }
        }
    }
}
