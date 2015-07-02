package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode;

/**
 * {@link StatligLoennstrinnperiodeOversetter} representerer algoritma
 * for å mappe om og konvertere statlige lønnstrinn til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode}
 * <p>
 * Informasjon henta frå statlige lønnstrinnperioder skal inneholde følgjande verdiar, alle representert som tekst:
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
 * <td>SPK_LTR</td>
 * <td>Typeindikator som identifiserer rada som ei statlig lønnstrinnperiode</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1 - 3-sifra tall</td>
 * <td>Lønnstrinn</td>
 * <td>TORT011.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato</td>
 * <td>TORT011.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT011.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>Kronebeløpet lønnstrinnet tilsvarar innanfor den aktuelle tidsperioda</td>
 * <td>TORT011.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class StatligLoennstrinnperiodeOversetter extends ReflectiveCsvOversetter<StatligLoennstrinnperiodeCsv, StatligLoennstrinnperiode> implements CsvOversetter<StatligLoennstrinnperiode> {

    public StatligLoennstrinnperiodeOversetter() {
        super("SPK_LTR", StatligLoennstrinnperiodeCsv.class);
    }

    @Override
    protected StatligLoennstrinnperiode transformer(StatligLoennstrinnperiodeCsv csvRad) {
        return new StatligLoennstrinnperiode(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.loennstrinn.map(Loennstrinn::new).get(),
                csvRad.beloep.map(Integer::valueOf).map(Kroner::new).get()
        );
    }
}
