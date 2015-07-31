package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;

/**
 * {@link MedregningsOversetter} representerer algoritma
 * for � mappe om og konvertere medregningar til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode}
 * <p>
 * Informasjon henta fr� medregningane skal inneholde f�lgjande verdiar, alle representert som tekst:
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
 * <td>2</td>
 * <td>Typeindikator som identifiserer rada som ei stillingsendring</td>
 * <td>Hardkoda, tallet 2</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>F�dselsdato for medlem</td>
 * <td>TORT014.DAT_KUNDE_FOEDT_NUM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>5-sifra tall</td>
 * <td>Personnummer for medlem</td>
 * <td>TORT014.IDE_KUNDE_PRSNR</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>Stillingsforholdnr</td>
 * <td>TORT014.IDE_SEKV_TORT125</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>yyyy.MM.dd</td>
 * <td>Fr� og med-dato</td>
 * <td>TORT014.DAT_FRA</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>yyyy.MM.dd</td>
 * <td>Til og med-dato</td>
 * <td>TORT014.DAT_TIL</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>2-sifra kode</td>
 * <td>Medregninskoda som n�rmare beskriv kva type medregning det er snakk om</td>
 * <td>TORT014.TYP_KODE</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>L�nn</td>
 * <td>TORT014.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class MedregningsOversetter extends ReflectiveCsvOversetter<MedregningCsv, Medregningsperiode> implements MedlemsdataOversetter<Medregningsperiode> {
    public MedregningsOversetter() {
        super("2", MedregningCsv.class);
    }

    @Override
    protected Medregningsperiode transformer(MedregningCsv csvRad) {
        return Medregningsperiode.medregning()
                .fraOgMed(csvRad.fraOgMedDato.map(Datoar::dato).get())
                .tilOgMed(csvRad.tilOgMedDato.map(Datoar::dato))
                .beloep(csvRad.loenn.map(Integer::valueOf).map(Kroner::new).map(Medregning::new).get())
                .kode(csvRad.kode.map(Integer::valueOf).map(Medregningskode::valueOf).get())
                .stillingsforhold(csvRad.stillingsforhold.map(Long::valueOf).map(StillingsforholdId::valueOf).get())
                .foedselsdato(csvRad.foedselsdato.map(Integer::valueOf).map(Foedselsdato::foedselsdato).get())
                .personnummer(csvRad.personnummer.map(Integer::valueOf).map(Personnummer::new).get())
                .bygg();
    }
}
