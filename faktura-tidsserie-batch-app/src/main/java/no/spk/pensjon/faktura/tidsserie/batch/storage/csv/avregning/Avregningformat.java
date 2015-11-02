package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.avregning;

import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.AFP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.GRU;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.PEN;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.TIP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.YSK;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.AFPPremieRegel;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.GRUPremieRegel;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.PENPremieRegel;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.TIPPremieRegel;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.YSKPremieRegel;
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
 * {@link Avregningformat} representerer mapping-strategien for konvertering av {@link Underlagsperiode}r til
 * CSV-formaterte rader for innmating i avregningstidsserien til TORT277 ved generering av nye avregningsutkast.
 * <br>
 * Nye kolonner skal alltid leggast til etter siste eksisterande kolonne i formatet.
 * <br>
 * Formatendringar på eksisterande kolonner bør ein unngå i så stor grad som mulig. Fortrinnsvis bør ein vurdere
 * å beholde gamal kolonne på gamalt format og legge til ny kolonne for nytt format.
 * <br>
 * Merk at ei kvar endring på eksisterande kolonner i formatet må sjekkast opp mot tabelldefinisjonen for TORT277 i
 * forhold til kva den støttar av verdityper, lengder, antall desimalar og liknande. Dersom endringa medfører at
 * betydninga eller tolkinga av verdiar frå kolonner eksponert via TOVIXXX-viewa, må det og avklarast mot Team Polaris
 * sidan EDW og DVH-jobbane deira kan komme til å kreve endring. Ergo: Unngå endring på eksisterande kolonner i så stor
 * grad som mulig.
 * <br>
 * Merk at foreløpig handhevar ikkje formatet kontrakta fullt ut med tanke på lengdeavgrensing av felt som potensielt
 * sett kan ha verdiar som blir lenger enn det kontrakta tillater.
 *
 * @author Tarjei Skorgenes
 * @since 1.2.0
 */
public class Avregningformat implements CSVFormat {
    private final Desimaltallformatering desimaltall = new Desimaltallformatering();

    private final ThreadLocal<FormatSpesifikasjon> s = ThreadLocal.withInitial(() -> new FormatSpesifikasjon(desimaltall) {
        protected void definer() {
            kolonne(1, "DAT_OBSERVASJON", (u, up) -> dato(u.annotasjonFor(Observasjonsdato.class).dato())).obligatorisk();
            kolonne(2, "DAT_FOM", (u, up) -> dato(up.fraOgMed())).obligatorisk();
            kolonne(3, "DAT_TOM", (u, up) -> dato(up.tilOgMed().get())).obligatorisk();
            kolonne(4, "DAT_KUNDE_FOEDT_NUM", (u, up) -> kode(up.annotasjonFor(Foedselsnummer.class).toString().substring(0, 8))).obligatorisk();
            kolonne(5, "IDE_KUNDE_PRSNR", (u, up) -> kode(up.annotasjonFor(Foedselsnummer.class).toString().substring(8, 13))).obligatorisk();
            kolonne(6, "IDE_SEKV_TORT125", (u, up) -> heiltall(up.annotasjonFor(StillingsforholdId.class).id())).obligatorisk();
            kolonne(7, "NUM_AVTALE_ID", (u, up) -> heiltall(up.annotasjonFor(AvtaleId.class).id())).obligatorisk();
            kolonne(8, "IDE_ARBGIV_NR", (u, up) -> kode(up.valgfriAnnotasjonFor(Orgnummer.class).map(Orgnummer::id)));
            kolonne(9, "IDE_PENS_ORD", (u, up) -> kode(up.valgfriAnnotasjonFor(Ordning.class).map(Ordning::kode)));
            kolonne(10, "IDE_AVTALE_PREMIEST", (u, up) -> kode(up.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)));
            kolonne(11, "TYP_AKSJONSKODE", (u, up) -> kode(up.valgfriAnnotasjonFor(Aksjonskode.class).map(Aksjonskode::kode)));
            kolonne(12, "NUM_STILLINGSKODE", (u, up) -> kode(up.valgfriAnnotasjonFor(Stillingskode.class).map(Stillingskode::getKode)));
            kolonne(13, "RTE_DELTID", (u, up) -> prosent(up.valgfriAnnotasjonFor(Stillingsprosent.class).map(Stillingsprosent::prosent), 3));
            kolonne(14, "NUM_LTR", (u, up) -> kode(up.valgfriAnnotasjonFor(Loennstrinn.class).map(Loennstrinn::trinn)));
            kolonne(15, "BEL_LONN", (u, up) -> beloep(up.valgfriAnnotasjonFor(LoennstrinnBeloep.class).map(LoennstrinnBeloep::beloep)));
            kolonne(16, "BEL_DELTIDSJUSTERT_LONN", (u, up) -> beloep(up.valgfriAnnotasjonFor(DeltidsjustertLoenn.class).map(DeltidsjustertLoenn::beloep)));
            kolonne(17, "BEL_FTILL", (u, up) -> beloep(up.valgfriAnnotasjonFor(Fastetillegg.class).map(Fastetillegg::beloep)));
            kolonne(18, "BEL_VTILL", (u, up) -> beloep(up.valgfriAnnotasjonFor(Variabletillegg.class).map(Variabletillegg::beloep)));
            kolonne(19, "BEL_FUTILL", (u, up) -> beloep(up.valgfriAnnotasjonFor(Funksjonstillegg.class).map(Funksjonstillegg::beloep)));
            kolonne(20, "BEL_LONN_MDR", (u, up) -> beloep(up.valgfriAnnotasjonFor(Medregning.class).map(Medregning::beloep)));
            kolonne(21, "TYP_KODE_MDR", (u, up) -> kode(up.valgfriAnnotasjonFor(Medregningskode.class).map(Medregningskode::kode)));
            kolonne(22, "BEL_G", (u, up) -> beloep(up.valgfriAnnotasjonFor(Grunnbeloep.class).map(Grunnbeloep::beloep)));
            kolonne(23, "RTE_REGEL_AARSFAKTOR", (u, up) -> prosent(up.beregn(AarsfaktorRegel.class).tilProsent(), 7));
            kolonne(24, "NUM_REGEL_AARSLENGDE", (u, up) -> heiltall(up.beregn(AarsLengdeRegel.class).verdi()));
            kolonne(25, "RTE_REGEL_AARSVERK", (u, up) -> prosent(up.beregn(AarsverkRegel.class).tilProsent(), 2));
            kolonne(26, "NUM_REGEL_ANTALLDAGER", (u, up) -> heiltall(up.beregn(AntallDagarRegel.class).verdi()));
            kolonne(27, "BEL_REGEL_DELTIDSJUSTERT_LONN", (u, up) -> beloep(up.beregn(DeltidsjustertLoennRegel.class)));
            kolonne(28, "BEL_REGEL_LONN_TILLEGG", (u, up) -> beloep(up.beregn(LoennstilleggRegel.class)));
            kolonne(29, "BEL_REGEL_PENSJONSGIVENDE_LONN", (u, up) -> beloep(up.beregn(MaskineltGrunnlagRegel.class)));
            kolonne(30, "BEL_REGEL_MEDREGNING", (u, up) -> beloep(up.beregn(MedregningsRegel.class)));
            kolonne(31, "RTE_REGEL_MINSTEGRENSE", (u, up) -> prosent(up.beregn(MinstegrenseRegel.class).grense(), 2));
            kolonne(32, "NUM_REGEL_OEVRE_LONNSGRENSE", (u, up) -> beloep(up.beregn(OevreLoennsgrenseRegel.class)));
            kolonne(33, "RTE_REGEL_GRUPPELIV", (u, up) -> prosent(up.beregn(GruppelivsfaktureringRegel.class).andel(), 4));
            kolonne(34, "RTE_REGEL_YRKESSKADE", (u, up) -> prosent(up.beregn(YrkesskadefaktureringRegel.class).andel(), 4));
            kolonne(35, "FLG_REGEL_MEDREGNING", (u, up) -> flagg(up.beregn(ErMedregningRegel.class)));
            kolonne(36, "FLG_REGEL_PERMISJON_UTEN_LONN", (u, up) -> flagg(up.beregn(ErPermisjonUtanLoennRegel.class)));
            kolonne(37, "FLG_REGEL_UNDER_MINSTEGRENSE", (u, up) -> flagg(up.beregn(ErUnderMinstegrensaRegel.class)));
            kolonne(38, "FLG_PEN", (u, up) -> flagg(erFakturerbar(up, PEN)));
            kolonne(39, "RTE_PEN_ARBANDEL", (u, up) -> prosent(prosentsatser(up, PEN).map(Satser::arbeidsgiverpremie), 2));
            kolonne(40, "RTE_PEN_MEDL_ANDEL", (u, up) -> prosent(prosentsatser(up, PEN).map(Satser::medlemspremie), 2));
            kolonne(41, "RTE_PEN_ADMGEB", (u, up) -> prosent(prosentsatser(up, PEN).map(Satser::administrasjonsgebyr), 2));
            kolonne(42, "KOD_PEN_PRODUKTINFO", (u, up) -> kode(produktinfo(up, PEN)));
            kolonne(43, "FLG_AFP", (u, up) -> flagg(erFakturerbar(up, AFP)));
            kolonne(44, "RTE_AFP_ARBANDEL", (u, up) -> prosent(prosentsatser(up, AFP).map(Satser::arbeidsgiverpremie), 2));
            kolonne(45, "RTE_AFP_MEDL_ANDEL", (u, up) -> prosent(prosentsatser(up, AFP).map(Satser::medlemspremie), 2));
            kolonne(46, "RTE_AFP_ADMGEB", (u, up) -> prosent(prosentsatser(up, AFP).map(Satser::administrasjonsgebyr), 2));
            kolonne(47, "KOD_AFP_PRODUKTINFO", (u, up) -> kode(produktinfo(up, AFP)));
            kolonne(48, "FLG_TIP", (u, up) -> flagg(erFakturerbar(up, TIP)));
            kolonne(49, "RTE_TIP_ARBANDEL", (u, up) -> prosent(prosentsatser(up, TIP).map(Satser::arbeidsgiverpremie), 2));
            kolonne(50, "RTE_TIP_MEDL_ANDEL", (u, up) -> prosent(prosentsatser(up, TIP).map(Satser::medlemspremie), 2));
            kolonne(51, "RTE_TIP_ADMGEB", (u, up) -> prosent(prosentsatser(up, TIP).map(Satser::administrasjonsgebyr), 2));
            kolonne(52, "KOD_TIP_PRODUKTINFO", (u, up) -> kode(produktinfo(up, TIP)));
            kolonne(53, "FLG_GRU", (u, up) -> flagg(erFakturerbar(up, GRU)));
            kolonne(54, "BEL_GRU_ARBANDEL", (u, up) -> beloep(beloepsatser(up, GRU).map(Satser::arbeidsgiverpremie)));
            kolonne(55, "BEL_GRU_MEDL_ANDEL", (u, up) -> beloep(beloepsatser(up, GRU).map(Satser::medlemspremie)));
            kolonne(56, "BEL_GRU_ADMGEB", (u, up) -> beloep(beloepsatser(up, GRU).map(Satser::administrasjonsgebyr)));
            kolonne(57, "KOD_GRU_PRODUKTINFO", (u, up) -> kode(produktinfo(up, GRU)));
            kolonne(58, "FLG_YSK", (u, up) -> flagg(erFakturerbar(up, YSK)));
            kolonne(59, "BEL_YSK_ARBANDEL", (u, up) -> beloep(beloepsatser(up, YSK).map(Satser::arbeidsgiverpremie)));
            kolonne(60, "BEL_YSK_MEDL_ANDEL", (u, up) -> beloep(beloepsatser(up, YSK).map(Satser::medlemspremie)));
            kolonne(61, "BEL_YSK_ADMGEB", (u, up) -> beloep(beloepsatser(up, YSK).map(Satser::administrasjonsgebyr)));
            kolonne(62, "KOD_YSK_PRODUKTINFO", (u, up) -> kode(produktinfo(up, YSK)));
            kolonne(63, "KOD_YSK_RISIKO_KL", (u, up) -> kode(avtale(up).flatMap(Avtale::risikoklasse)));
            kolonne(64, "IDE_UUID", (u, up) -> up.id().toString()).obligatorisk();
            kolonne(65, "NUM_ANTALLFEIL", (u, up) -> antallFeil()).obligatorisk();
            kolonne(66, "IDE_SEKV_TORT129", (u, up) -> kode(up.valgfriAnnotasjonFor(ArbeidsgiverId.class).map(ArbeidsgiverId::id)));
            kolonne(67, "IDE_TIDSSERIENUMMER", (u, up) -> kode(u.valgfriAnnotasjonFor(Tidsserienummer.class)));
            kolonne(68, "KOD_TERMINTYPE", (u, up) -> kode(up.beregn(TermintypeRegel.class).kode()));
            kolonne(69, "IDE_LINJE_NR", (u, up) -> kode(up.valgfriAnnotasjonFor(Medlemslinjenummer.class)));
            kolonne(70, "TYP_PREMIEKATEGORI", (u, up) -> kode(up.valgfriAnnotasjonFor(Premiekategori.class).map(Premiekategori::kode)));
            kolonne(71, "IDE_AVREGNING_VERSJON", (u, up) -> kode(up.annotasjonFor(Avregningsversjon.class).toString())).obligatorisk();
            kolonne(72, "BEL_PEN_PREMIE_MEDL", (u, up) -> premie(up.beregn(PENPremieRegel.class).arbeidsgiver()));
            kolonne(73, "BEL_PEN_PREMIE_ARBGIV", (u, up) -> premie(up.beregn(PENPremieRegel.class).medlem()));
            kolonne(74, "BEL_PEN_PREMIE_ADM_GEB", (u, up) -> premie(up.beregn(PENPremieRegel.class).administrasjonsgebyr()));
            kolonne(75, "BEL_AFP_PREMIE_MEDL", (u, up) -> premie(up.beregn(AFPPremieRegel.class).arbeidsgiver()));
            kolonne(76, "BEL_AFP_PREMIE_ARBGIV", (u, up) -> premie(up.beregn(AFPPremieRegel.class).medlem()));
            kolonne(77, "BEL_AFP_PREMIE_ADM_GEB", (u, up) -> premie(up.beregn(AFPPremieRegel.class).administrasjonsgebyr()));
            kolonne(78, "BEL_TIP_PREMIE_MEDL", (u, up) -> premie(up.beregn(TIPPremieRegel.class).arbeidsgiver()));
            kolonne(79, "BEL_TIP_PREMIE_ARBGIV", (u, up) -> premie(up.beregn(TIPPremieRegel.class).medlem()));
            kolonne(80, "BEL_TIP_PREMIE_ADM_GEB", (u, up) -> premie(up.beregn(TIPPremieRegel.class).administrasjonsgebyr()));
            kolonne(81, "BEL_GRU_PREMIE_MEDL", (u, up) -> premie(up.beregn(GRUPremieRegel.class).arbeidsgiver()));
            kolonne(82, "BEL_GRU_PREMIE_ARBGIV", (u, up) -> premie(up.beregn(GRUPremieRegel.class).medlem()));
            kolonne(83, "BEL_GRU_PREMIE_ADM_GEB", (u, up) -> premie(up.beregn(GRUPremieRegel.class).administrasjonsgebyr()));
            kolonne(84, "BEL_YSK_PREMIE_MEDL", (u, up) -> premie(up.beregn(YSKPremieRegel.class).arbeidsgiver()));
            kolonne(85, "BEL_YSK_PREMIE_ARBGIV", (u, up) -> premie(up.beregn(YSKPremieRegel.class).medlem()));
            kolonne(86, "BEL_YSK_PREMIE_ADM_GEB", (u, up) -> premie(up.beregn(YSKPremieRegel.class).administrasjonsgebyr()));
        }
    });

    @Override
    public Stream<String> kolonnenavn() {
        return s.get().kolonnenavn();
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        return s.get().serialiser(observasjonsunderlag, p);
    }
}
