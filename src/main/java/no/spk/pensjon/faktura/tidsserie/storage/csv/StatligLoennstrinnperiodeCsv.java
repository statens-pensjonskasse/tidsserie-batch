package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class StatligLoennstrinnperiodeCsv {
    @CsvIndex(1)
    Optional<String> loennstrinn;
    @CsvIndex(2)
    Optional<String> fraOgMedDato;
    @CsvIndex(3)
    Optional<String> tilOgMedDato;
    @CsvIndex(4)
    Optional<String> beloep;
}
