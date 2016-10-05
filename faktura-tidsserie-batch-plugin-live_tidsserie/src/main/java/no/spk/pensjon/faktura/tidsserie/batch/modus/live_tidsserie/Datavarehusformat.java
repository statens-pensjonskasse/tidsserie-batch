package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode.PERMISJON_UTAN_LOENN;

import java.util.Set;
import java.util.UUID;
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
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsverkRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.DeltidsjustertLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErUnderMinstegrensaRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.GruppelivsfaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.LoennstilleggRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MaskineltGrunnlagRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MedregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MinstegrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.OevreLoennsgrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.TermintypeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.UUIDRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.YrkesskadefaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;

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
 * <a href="http://wiki/confluence/display/dok/faktura-tidsserie-batch+-+CSV-format+for+DVH+og+Qlikview">faktura-tidsserie-batch - CSV-format for DVH og
 * Qlikview</a>
 *
 * @author Tarjei Skorgenes
 */
public class Datavarehusformat implements CSVFormat {
    private final Desimaltallformatering desimaltall = new Desimaltallformatering();

    private final Premiesatskolonner premiesatskolonner = new Premiesatskolonner();

    private final ThreadLocal<FormatSpesifikasjon> spesifikasjon = ThreadLocal.withInitial(() -> new FormatSpesifikasjon(desimaltall) {
        protected void definer() {
            kolonne("observasjonsdato", (u, up) -> dato(u.annotasjonFor(Observasjonsdato.class).dato())).obligatorisk();
            kolonne("fraOgMedDato", (u, up) -> dato(up.fraOgMed())).obligatorisk();
            kolonne("tilOgMedDato", (u, up) -> dato(up.tilOgMed().get())).obligatorisk();
            kolonne("medlem", (u, up) -> kode(up.annotasjonFor(Foedselsnummer.class).toString())).obligatorisk();
            kolonne("stillingsforhold", (u, up) -> heiltall(up.annotasjonFor(StillingsforholdId.class).id())).obligatorisk();
            kolonne("avtale", (u, up) -> heiltall(up.annotasjonFor(AvtaleId.class).id())).obligatorisk();
            kolonne("organisasjonsnummer", (u, up) -> kode(up.valgfriAnnotasjonFor(Orgnummer.class).map(Orgnummer::id)));
            kolonne("ordning", (u, up) -> kode(up.valgfriAnnotasjonFor(Ordning.class).map(Ordning::kode)));
            kolonne("premiestatus", (u, up) -> kode(up.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)));
            kolonne("aksjonskode", (u, up) -> kode(up.valgfriAnnotasjonFor(Aksjonskode.class).map(Aksjonskode::kode)));
            kolonne("stillingskode", (u, up) -> kode(up.valgfriAnnotasjonFor(Stillingskode.class).map(Stillingskode::getKode)));
            kolonne("stillingsprosent", (u, up) -> prosent(up.valgfriAnnotasjonFor(Stillingsprosent.class).map(Stillingsprosent::prosent), 3));
            kolonne("loennstrinn", (u, up) -> kode(up.valgfriAnnotasjonFor(Loennstrinn.class).map(Loennstrinn::trinn)));
            kolonne("loennstrinnBeloep", (u, up) -> beloep(up.valgfriAnnotasjonFor(LoennstrinnBeloep.class).map(LoennstrinnBeloep::beloep)));
            kolonne("deltidsjustertLoenn", (u, up) -> beloep(up.valgfriAnnotasjonFor(DeltidsjustertLoenn.class).map(DeltidsjustertLoenn::beloep)));
            kolonne("fasteTillegg", (u, up) -> beloep(up.valgfriAnnotasjonFor(Fastetillegg.class).map(Fastetillegg::beloep)));
            kolonne("variableTillegg", (u, up) -> beloep(up.valgfriAnnotasjonFor(Variabletillegg.class).map(Variabletillegg::beloep)));
            kolonne("funksjonsTillegg", (u, up) -> beloep(up.valgfriAnnotasjonFor(Funksjonstillegg.class).map(Funksjonstillegg::beloep)));
            kolonne("medregning", (u, up) -> beloep(up.valgfriAnnotasjonFor(Medregning.class).map(Medregning::beloep)));
            kolonne("medregningskode", (u, up) -> kode(up.valgfriAnnotasjonFor(Medregningskode.class).map(Medregningskode::kode)));
            kolonne("grunnbeloep", (u, up) -> beloep(up.valgfriAnnotasjonFor(Grunnbeloep.class).map(Grunnbeloep::beloep)));
            kolonne("regel_aarsfaktor", (u, up) -> prosent(up.beregn(AarsfaktorRegel.class).tilProsent(), 8));
            kolonne("regel_aarslengde", (u, up) -> heiltall(up.beregn(AarsLengdeRegel.class).verdi()));
            kolonne("regel_aarsverk", (u, up) -> prosent(up.beregn(AarsverkRegel.class).tilProsent(), 2));
            kolonne("regel_antalldager", (u, up) -> heiltall(up.beregn(AntallDagarRegel.class).verdi()));
            kolonne("regel_deltidsjustertloenn", (u, up) -> beloep(up.beregn(DeltidsjustertLoennRegel.class)));
            kolonne("regel_loennstillegg", (u, up) -> beloep(up.beregn(LoennstilleggRegel.class)));
            kolonne("regel_pensjonsgivende_loenn", (u, up) -> beloep(up.beregn(MaskineltGrunnlagRegel.class)));
            kolonne("regel_medregning", (u, up) -> beloep(up.beregn(MedregningsRegel.class)));
            kolonne("regel_minstegrense", (u, up) -> prosent(up.beregn(MinstegrenseRegel.class).grense(), 2));
            kolonne("regel_oevreLoennsgrense", (u, up) -> beloep(up.beregn(OevreLoennsgrenseRegel.class)));
            kolonne("regel_gruppelivsandel", (u, up) -> prosent(up.beregn(GruppelivsfaktureringRegel.class).andel(), 4));
            kolonne("regel_yrkesskadeandel", (u, up) -> prosent(up.beregn(YrkesskadefaktureringRegel.class).andel(), 4));
            kolonne("regel_erMedregning", (u, up) -> flagg(up.valgfriAnnotasjonFor(Medregning.class).isPresent()));
            kolonne("regel_erPermisjonUtenLoenn", (u, up) -> flagg(up.valgfriAnnotasjonFor(Aksjonskode.class).filter(kode -> kode.equals(PERMISJON_UTAN_LOENN)).isPresent()));
            kolonne("regel_erUnderMinstegrensen", (u, up) -> flagg(up.beregn(ErUnderMinstegrensaRegel.class)));
            kolonnerForPremiesatser(Produkt.PEN);
            kolonnerForPremiesatser(Produkt.AFP);
            kolonnerForPremiesatser(Produkt.TIP);
            kolonnerForPremiesatser(Produkt.GRU);
            kolonnerForPremiesatser(Produkt.YSK);
            kolonne("produkt_YSK_risikoklasse", (u, up) -> kode(up.valgfriAnnotasjonFor(Avtale.class).flatMap(Avtale::risikoklasse)));
            kolonne("uuid", (u, up) -> up.annotasjonFor(UUID.class).toString()).obligatorisk();
            kolonne("antallFeil", (u, up) -> ANTALL_FEIL_PLACEHOLDER).obligatorisk();
            kolonne("arbeidsgivernummer", (u, up) -> kode(up.valgfriAnnotasjonFor(ArbeidsgiverId.class).map(ArbeidsgiverId::id)));
            kolonne("tidsserienummer", (u, up) -> kode(u.valgfriAnnotasjonFor(Tidsserienummer.class)));
            kolonne("termintype", (u, up) -> kode(up.beregn(TermintypeRegel.class).kode()));
            kolonne("linjenummer_historikk", (u, up) -> kode(up.valgfriAnnotasjonFor(Medlemslinjenummer.class)));
            kolonne("premiekategori", (u, up) -> kode(up.valgfriAnnotasjonFor(Premiekategori.class).map(Premiekategori::kode)));
        }

        private void kolonnerForPremiesatser(final Produkt produkt) {
            final PremiesatskolonneIterator produktKolonner = new PremiesatskolonneIterator(premiesatskolonner, produkt);
            kolonnenavnForProdukt(produkt)
                    .forEach(
                            kolonnenavn -> kolonne(kolonnenavn, (u, up) -> produktKolonner.nestePremiesatsverdiFor(up))
                    );
        }

        private Stream<String> kolonnenavnForProdukt(final Produkt produkt) {
            return Stream.of(
                    "produkt_" + produkt.kode(),
                    "produkt_" + produkt.kode() + "_satsArbeidsgiver",
                    "produkt_" + produkt.kode() + "_satsMedlem",
                    "produkt_" + produkt.kode() + "_satsAdministrasjonsgebyr",
                    "produkt_" + produkt.kode() + "_produktinfo"
            );
        }
    });

    @Override
    public Stream<String> kolonnenavn() {
        return spesifikasjon.get().kolonnenavn();
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        return spesifikasjon.get().serialiser(observasjonsunderlag, p);
    }

    @Override
    public Stream<Object> serialiser(Underlag observasjonsunderlag, Underlagsperiode periode, Set<String> kolonnenavnfilter) {
        return spesifikasjon.get().serialiser(observasjonsunderlag, periode, k -> kolonnenavnfilter.contains(k.name()));
    }
}
