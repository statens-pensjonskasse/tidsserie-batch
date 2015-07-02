package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.time.LocalDate;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.DeltidsjustertLoenn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Fastetillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Funksjonstillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;

/**
 * {@link StillingsendringOversetter} representerer algoritma
 * for � mappe om og konvertere stillingshistorikk til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring}
 * <p>
 * Informasjon henta fr� stillingshistorikken skal inneholde f�lgjande verdiar, alle representert som tekst:
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Kilde</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>0</td>
 * <td>Typeindikator som identifiserer rada som ei stillingsendring</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>F�dselsdato for medlem</td>
 * <td>TORT016.DAT_KUNDE_FOEDT_NUM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>5-sifra tall</td>
 * <td>Personnummer for medlem</td>
 * <td>TORT016.IDE_KUNDE_PRSNR</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>Stillingsforholdnr</td>
 * <td>TORT016.IDE_SEKV_TORT125</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>4-sifra kode</td>
 * <td>Aksjonskoda som n�rmare beskrive kva type stillingsendring det er snakk om</td>
 * <td>TORT016.TYP_AKSJONSKODE</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Long</td>
 * <td>Organisasjonsnummer, ikkje i bruk</td>
 * <td>TORT016.IDE_ARBGIV_NR</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>3-sifra kode</td>
 * <td>Permisjonsavtale for stillingsendringar med aksjonskode 028, 029, 012</td>
 * <td>TORT016.TYP_PERMAVT</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>yyyy.MM.dd</td>
 * <td>Registreringsdato for n�r stillingsendringa vart registrert i PUMA</td>
 * <td>TORT016.DAT_REGISTRERT</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>Double</td>
 * <td>Stillingsprosent for stillinga endringa er tilknytta, er normalt sett ein verdi mellom 0 og 100, men kan for visse historiske �rgangar og
 * yrkesgrupper vere st�rre enn 100</td>
 * <td>TORT016.RTE_DELTID</td>
 * </tr>
 * <tr>
 * <td>9</td>
 * <td>Integer</td>
 * <td>L�nnstrinn, for stillingar som ikkje innrapporterer l�nn blir l�nna innrapportert som l�nnstrinn som kan benyttast for � sl� opp l�nn i 100%
 * stilling.</td>
 * <td>TORT016.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>10</td>
 * <td>Integer</td>
 * <td>Deltidsjustert, innrapportert l�nn for stillingar som ikkje blir innrapportert med l�nnstrinn</td>
 * <td>TORT016.BEL_LONN</td>
 * </tr>
 * <tr>
 * <td>11</td>
 * <td>Integer</td>
 * <td>Faste l�nnstillegg som blir utbetalt i tillegg til grunnl�nna, skal innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_FTILL</td>
 * </tr>
 * <tr>
 * <td>12</td>
 * <td>Integer</td>
 * <td>Variable l�nnstillegg som blir utbetalt i tillegg til grunnl�nna, skal innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_VTILL</td>
 * </tr>
 * <tr>
 * <td>13</td>
 * <td>Integer</td>
 * <td>Funksjonstillegg som blir utbetalt i tillegg til grunnl�nna, skal ikkje innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_FUTILL</td>
 * </tr>
 * <tr>
 * <td>14</td>
 * <td>yyyyMMdd</td>
 * <td>Aksjonsdato, datoen stillingsendringa trer i kraft</td>
 * <td>TORT016.DAT_AKSJON</td>
 * </tr>
 * <tr>
 * <td>15</td>
 * <td>integer</td>
 * <td>Stillingskode</td>
 * <td>TORT016.NUM_STILLINGSKODE</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class StillingsendringOversetter extends ReflectiveCsvOversetter<StillingsendringCsv, Stillingsendring> implements MedlemsdataOversetter<Stillingsendring> {

    private final OversetterSupport support = new OversetterSupport();

    public StillingsendringOversetter() {
        super("0", StillingsendringCsv.class);
    }

    @Override
    protected Stillingsendring transformer(StillingsendringCsv csvRad) {
        return new Stillingsendring()
                .foedselsdato(csvRad.foedselsdato.map(Datoar::dato).map(Foedselsdato::new).get())
                .personnummer(csvRad.personnummer.map(Integer::valueOf).map(Personnummer::new).get())
                .stillingsforhold(csvRad.stillingsforhold.map(StillingsforholdId::valueOf).get())
                .aksjonskode(csvRad.aksjonskode.map(Aksjonskode::valueOf).orElse(Aksjonskode.UKJENT))
                .aksjonsdato(tilDato(csvRad.aksjonsdato).get())
                .stillingsprosent(csvRad.stillingsprosent.map(Prosent::new).map(Stillingsprosent::new).get())
                .stillingskode(csvRad.stillingskode.map(Stillingskode::parse))
                .loennstrinn(readLoennstrinn(csvRad.loennstrinn))
                .loenn(csvRad.loenn.map(Long::valueOf).map(Kroner::new).map(DeltidsjustertLoenn::new))
                .fastetillegg(readFastetillegg(csvRad.fasteTillegg))
                .variabletillegg(readVariabletillegg(csvRad.variableTillegg))
                .funksjonstillegg(readFunksjonstillegg(csvRad.funksjonstillegg));
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til l�nnstrinn.
     *
     * @param string med verdi
     * @return endringas l�nnstrinn eller ingenting dersom l�nnstrinn manglar, er tomt eller er lik 0
     */
    Optional<Loennstrinn> readLoennstrinn(Optional<String> string) {
        return string
                .map(Integer::valueOf)
                .filter(tall -> tall > 0)
                .map(Loennstrinn::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til faste tillegg.
     *
     * @param string med verdi
     * @return endringas faste tillegg eller ingenting dersom faste tillegg manglar, er tomt eller er lik 0
     */
    Optional<Fastetillegg> readFastetillegg(Optional<String> string) {
        return readValgfrittKronebeloep(string).map(Fastetillegg::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til variable tillegg.
     *
     * @param string med verdi
     * @return endringas variable tillegg eller ingenting dersom variable tillegg manglar, er tomt eller er lik 0
     */
    Optional<Variabletillegg> readVariabletillegg(Optional<String> string) {
        return readValgfrittKronebeloep(string).map(Variabletillegg::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til funksjonstillegg.
     *
     * @param string med verdi
     * @return endringas variable tillegg eller ingenting dersom funksjonstillegg manglar, er tomt eller er lik 0
     */
    Optional<Funksjonstillegg> readFunksjonstillegg(Optional<String> string) {
        return readValgfrittKronebeloep(string).map(Funksjonstillegg::new);
    }

    /**
     * @see OversetterSupport#tilDato(Optional) )
     */
    Optional<LocalDate> tilDato(Optional<String> string) {
        return support.tilDato(string);
    }

    private Optional<Kroner> readValgfrittKronebeloep(Optional<String> string) {
        return string
                .map(Integer::valueOf)
                .filter(tall -> tall > 0)
                .map(Kroner::new);
    }
}
