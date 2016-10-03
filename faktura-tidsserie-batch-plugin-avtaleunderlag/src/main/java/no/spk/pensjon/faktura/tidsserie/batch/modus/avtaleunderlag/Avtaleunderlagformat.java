package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.AFP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.GRU;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.PEN;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.TIP;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt.YSK;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Aarsfaktor;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.AntallDagar;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;

/**
 * {@link Avtaleunderlagformat} konverterer fra {@link Underlagsperiode}r til csv-formaterte rader,
 * <br>
 * Ny kolonner skal alltid legges til på slutten av eksisterende kolonne i formatet. Endringer i formatering bør gjøres med
 * omhu, da DVH skal benytte filene.
 * <br>
 * Wiki-dokumentasjon av formatet finnes på: http://wiki/confluence/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Avtaleunderlag
 * @author Snorre E. Brekke - Computas
 */
class Avtaleunderlagformat implements CSVFormat {
    private static final int PROSENT_DESIMALER = 2;
    private static final int AARSFAKTOR_DESIMALER = 8;
    private final Desimaltallformatering desimaltall = new Desimaltallformatering();

    private final ThreadLocal<FormatSpesifikasjon> s = ThreadLocal.withInitial(() -> new FormatSpesifikasjon(desimaltall) {
        protected void definer() {
            kolonne("premieaar", (u, p) -> p.annotasjonFor(Aarstall.class)).obligatorisk();
            kolonne("fraOgMedDato", (u, p) -> dato(p.fraOgMed())).obligatorisk();
            kolonne("tilOgMedDato", (u, p) -> dato(p.tilOgMed())).obligatorisk();
            kolonne("avtale", (u, p) -> kode(u.valgfriAnnotasjonFor(AvtaleId.class).map(AvtaleId::id))).obligatorisk();
            kolonne("uttrekksdato", (u, p) -> dato(u.valgfriAnnotasjonFor(Uttrekksdato.class).map(Uttrekksdato::uttrekksdato))).obligatorisk();
            kolonne("uuid", (u, p) -> p.annotasjonFor(UUID.class).toString()).obligatorisk();
            kolonne("antallFeil", (u, p) -> antallFeil()).obligatorisk();
            kolonne("tidsserienummer", (u, p) -> u.annotasjonFor(Tidsserienummer.class)).obligatorisk();

            kolonne("regel_aarsfaktor", (u, p) -> prosent(of(p.beregn(AarsfaktorRegel.class)).map(Aarsfaktor::tilProsent), AARSFAKTOR_DESIMALER));
            kolonne("regel_aarslengde", (u, p) -> kode(of(p.beregn(AarsLengdeRegel.class)).map(AntallDagar::verdi)));
            kolonne("regel_antalldager", (u, p) -> kode(of(p.beregn(AntallDagarRegel.class)).map(AntallDagar::verdi)));

            kolonne("ordning", (u, p) -> kode(avtale(p).flatMap(Avtale::ordning).map(Ordning::kode)));
            kolonne("arbeidsgivernummer", (u, p) -> kode(p.valgfriAnnotasjonFor(ArbeidsgiverId.class).map(ArbeidsgiverId::id)));
            kolonne("organisasjonsnummer", (u, p) -> kode(p.valgfriAnnotasjonFor(Orgnummer.class).map(Orgnummer::id)));

            kolonne("premiestatus", (u, p) -> kode(p.valgfriAnnotasjonFor(Premiestatus.class).map(Premiestatus::kode)));
            kolonne("premiekategori", (u, p) -> kode(p.valgfriAnnotasjonFor(Premiekategori.class).map(Premiekategori::kode)));

            kolonne("produkt_PEN", (u, p) -> flagg(erFakturerbar(p, PEN)));
            kolonne("produkt_PEN_satsArbeidsgiver", (u, p) -> prosent(arbeidsgiverpremie(p, PEN)));
            kolonne("produkt_PEN_satsMedlem", (u, p) -> prosent(medlemspremie(p, PEN)));
            kolonne("produkt_PEN_satsAdministrasjonsgebyr", (u, p) -> prosent(administrasjonsgebyr(p, PEN)));
            kolonne("produkt_PEN_satsTotal", (u, p) -> prosent(sumPremiesats(p, PEN)));
            kolonne("produkt_PEN_produktinfo", (u, p) -> kode(produktinfo(p, PEN)));

            kolonne("produkt_AFP", (u, p) -> flagg(erFakturerbar(p, AFP)));
            kolonne("produkt_AFP_satsArbeidsgiver", (u, p) -> prosent(arbeidsgiverpremie(p, AFP)));
            kolonne("produkt_AFP_satsMedlem", (u, p) -> prosent(medlemspremie(p, AFP)));
            kolonne("produkt_AFP_satsAdministrasjonsgebyr", (u, p) -> prosent(administrasjonsgebyr(p, AFP)));
            kolonne("produkt_AFP_satsTotal", (u, p) -> prosent(sumPremiesats(p, AFP)));
            kolonne("produkt_AFP_produktinfo", (u, p) -> kode(produktinfo(p, AFP)));

            kolonne("produkt_TIP", (u, p) -> flagg(erFakturerbar(p, TIP)));
            kolonne("produkt_TIP_satsArbeidsgiver", (u, p) -> prosent(arbeidsgiverpremie(p, TIP)));
            kolonne("produkt_TIP_satsMedlem", (u, p) -> prosent(medlemspremie(p, TIP)));
            kolonne("produkt_TIP_satsAdministrasjonsgebyr", (u, p) -> prosent(administrasjonsgebyr(p, TIP)));
            kolonne("produkt_TIP_satsTotal", (u, p) -> prosent(sumPremiesats(p, TIP)));
            kolonne("produkt_TIP_produktinfo", (u, p) -> kode(produktinfo(p, TIP)));

            kolonne("produkt_prosent_satsTotal", (u, p) -> prosent(sumProsentsatsTotal(p)));

            kolonne("produkt_GRU", (u, p) -> flagg(erFakturerbar(p, GRU)));
            kolonne("produkt_GRU_satsArbeidsgiver", (u, p) -> beloep(arbeidsgiverpremieBeloep(p, GRU)));
            kolonne("produkt_GRU_satsMedlem", (u, p) -> beloep(medlemspremieBeloep(p, GRU)));
            kolonne("produkt_GRU_satsAdministrasjonsgebyr", (u, p) -> beloep(administrasjonsBeloep(p, GRU)));
            kolonne("produkt_GRU_satsTotal", (u, p) -> beloep(sumPremiesatsBeloep(p, GRU)));
            kolonne("produkt_GRU_produktinfo", (u, p) -> kode(produktinfo(p, GRU)));

            kolonne("produkt_YSK", (u, p) -> flagg(erFakturerbar(p, YSK)));
            kolonne("produkt_YSK_satsArbeidsgiver", (u, p) -> beloep(arbeidsgiverpremieBeloep(p, YSK)));
            kolonne("produkt_YSK_satsMedlem", (u, p) -> beloep(medlemspremieBeloep(p, YSK)));
            kolonne("produkt_YSK_satsAdministrasjonsgebyr", (u, p) -> beloep(administrasjonsBeloep(p, YSK)));
            kolonne("produkt_YSK_satsTotal", (u, p) -> beloep(sumPremiesatsBeloep(p, YSK)));
            kolonne("produkt_YSK_produktinfo", (u, p) -> kode(produktinfo(p, YSK)));
            kolonne("produkt_YSK_risikoklasse", (u, p) -> kode(avtale(p).flatMap(Avtale::risikoklasse)));
        }

        private String prosent(Optional<Prosent> prosent) {
            return prosent(prosent, PROSENT_DESIMALER);
        }
    });

    @Override
    public Stream<String> kolonnenavn() {
        return s.get().kolonnenavn();
    }

    @Override
    public Stream<Object> serialiser(final Underlag observasjonsunderlag, final Underlagsperiode p) {
        return s.get().serialiser(observasjonsunderlag, p, k -> true);
    }

    @Override
    public Stream<Object> serialiser(Underlag observasjonsunderlag, Underlagsperiode periode, Set<String> kolonnenavnfilter) {
        return s.get().serialiser(observasjonsunderlag, periode, k-> kolonnenavnfilter.contains(k.name()));
    }

}
