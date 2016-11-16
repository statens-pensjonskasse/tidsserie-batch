package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode;

/**
 * {@link OmregningsperiodeOversetter} representerer algoritma
 * for å mappe om og konvertere omregningsperioder til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode}
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
 * <td>OMREGNING</td>
 * <td>Typeindikator som identifiserer rada som ei omregningsperiode</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato</td>
 * <td>TORT024.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT024.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Integer</td>
 * <td>Grunnbeløpet</td>
 * <td>TORT023.BEL_G</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class OmregningsperiodeOversetter extends ReflectiveCsvOversetter<OmregningCsv, Omregningsperiode> implements CsvOversetter<Omregningsperiode> {
    public OmregningsperiodeOversetter() {
        super("OMREGNING", OmregningCsv.class);
    }

    @Override
    protected Omregningsperiode transformer(OmregningCsv csvRad) {
        return new Omregningsperiode(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.grunnbeloep.map(Integer::valueOf).map(Kroner::new).get()
        );
    }
}
