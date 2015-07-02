package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class ApotekLoennstrinnperiodeCsv {
    @CsvIndex(1)
    Optional<String> fraOgMedDato;
    @CsvIndex(2)
    Optional<String> tilOgMedDato;
    @CsvIndex(3)
    Optional<String> loennstrinn;
    @CsvIndex(4)
    Optional<String> stillingskode;
    @CsvIndex(5)
    Optional<String> beloep;
}
