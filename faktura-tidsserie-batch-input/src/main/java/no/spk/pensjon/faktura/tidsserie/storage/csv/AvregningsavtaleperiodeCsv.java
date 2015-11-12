package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * {@link AvregningsavtaleperiodeCsv} representerer ei rad fr� ei CSV-fil som inneheld avregningsperioda
 * til avregningsutkastet ein tidsserie skal tilknyttast.
 * <br>
 * F�lgjande felt blir st�tta av CSV-formatet for avregningsperioder.
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Obligatorisk?</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>AVREGNINGSAVTALE</td>
 * <td>Typeindikator, identifiserer rada som ei avregningsperiode</td>
 * <td>Ja</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>YYYY</td>
 * <td>F�rste premie�r i avregningsperioda</td>
 * <td>Ja</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>YYYY</td>
 * <td>Siste premie�r i avregningsperioda</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Integer</td>
 * <td>Versjonsnummeret til avregningsversjonen perioda tilh�yrer</td>
 * <td>Ja</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>Avtalenummerer perioda gjelder</td>
 * <td>Ja</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Snorre E. Brekke
 * @since 1.2.0
 */
class AvregningsavtaleperiodeCsv {
    @CsvIndex(1)
    Optional<String> premieAarFra;

    @CsvIndex(2)
    Optional<String> premieAarTil;

    @CsvIndex(3)
    Optional<String> avregningsversjon;

    @CsvIndex(4)
    Optional<String> avtale;
}
