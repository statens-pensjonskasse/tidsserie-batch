package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * {@link AvregningsperiodeCsv} representerer ei rad frå ei CSV-fil som inneheld avregningsperioda
 * til avregningsutkastet ein tidsserie skal tilknyttast.
 * <br>
 * Følgjande felt blir støtta av CSV-formatet for avregningsperioder.
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
 * <td>AVREGNINGSPERIODE</td>
 * <td>Typeindikator, identifiserer rada som ei avregningsperiode</td>
 * <td>Ja</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>YYYY</td>
 * <td>Første premieår i avregningsperioda</td>
 * <td>Ja</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>YYYY</td>
 * <td>Siste premieår i avregningsperioda</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Integer</td>
 * <td>Versjonsnummeret til avregningsversjonen perioda tilhøyrer</td>
 * <td>Ja</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 * @since 1.2.0
 */
class AvregningsperiodeCsv {
    @CsvIndex(1)
    Optional<String> premieAarFra;

    @CsvIndex(2)
    Optional<String> premieAarTil;

    @CsvIndex(3)
    Optional<String> avregningsversjon;
}
