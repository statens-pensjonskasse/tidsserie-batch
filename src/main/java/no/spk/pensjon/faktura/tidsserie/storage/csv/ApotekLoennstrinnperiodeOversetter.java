package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.LoennstrinnBeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode;

/**
 * {@link ApotekLoennstrinnperiodeOversetter} representerer algoritma
 * for å mappe om og konvertere statlige lønnstrinn til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode}
 * <p>
 * Informasjon henta frå lønnstrinnperioder tilknytta Apotekordninga skal inneholde følgjande verdiar, alle representert
 * som tekst:
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Blir typisk henta frå</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>{@linkplain #TYPEINDIKATOR}</td>
 * <td>Typeindikator som identifiserer rada som ei statlig lønnstrinnperiode</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato</td>
 * <td>TORT012.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT012.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1 - 3-sifra tall</td>
 * <td>Lønnstrinn</td>
 * <td>TORT012.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>1 - 2-sifra tall</td>
 * <td>Stillingskode</td>
 * <td>TORT012.NUM_STILLINGSKODE</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Integer</td>
 * <td>Kronebeløpet lønnstrinnet tilsvarar innanfor den aktuelle tidsperioda</td>
 * <td>TORT012.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class ApotekLoennstrinnperiodeOversetter extends ReflectiveCsvOversetter<ApotekLoennstrinnperiodeCsv, ApotekLoennstrinnperiode> implements CsvOversetter<ApotekLoennstrinnperiode> {

    public ApotekLoennstrinnperiodeOversetter() {
        super("POA_LTR", ApotekLoennstrinnperiodeCsv.class);
    }

    @Override
    protected ApotekLoennstrinnperiode transformer(ApotekLoennstrinnperiodeCsv csvRad) {
        return new ApotekLoennstrinnperiode(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.loennstrinn.map(Loennstrinn::new).get(),
                csvRad.stillingskode.map(Stillingskode::parse).get(),
                csvRad.beloep.map(Integer::valueOf).map(Kroner::new).map(LoennstrinnBeloep::new).get()
        );
    }
}
