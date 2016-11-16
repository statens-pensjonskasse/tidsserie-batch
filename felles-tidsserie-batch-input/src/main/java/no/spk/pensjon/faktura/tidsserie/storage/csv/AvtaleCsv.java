package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleCsv {
    @CsvIndex(1)
    Optional<String> avtaleId;
    @CsvIndex(2)
    Optional<String> fraOgMed;
    @CsvIndex(3)
    Optional<String> tilOgMed;
    @CsvIndex(4)
    Optional<String> ordning;
    @CsvIndex(5)
    Optional<String> arbeidsgiverId;
}
