package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class AvtalekoblingCsv {
    @CsvIndex(3)
    Optional<String> stillingsforhold;
    @CsvIndex(4)
    Optional<String> startDatp;
    @CsvIndex(5)
    Optional<String> sluttDato;
    @CsvIndex(6)
    Optional<String> avtale;
    @CsvIndex(7)
    Optional<String> ordning;
}
