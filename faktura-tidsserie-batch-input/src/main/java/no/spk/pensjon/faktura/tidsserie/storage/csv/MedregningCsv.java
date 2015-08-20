package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class MedregningCsv {
    @CsvIndex(1)
    Optional<String> foedselsdato;
    @CsvIndex(2)
    Optional<String> personnummer;
    @CsvIndex(3)
    Optional<String> stillingsforhold;
    @CsvIndex(4)
    Optional<String> fraOgMedDato;
    @CsvIndex(5)
    Optional<String> tilOgMedDato;
    @CsvIndex(6)
    Optional<String> kode;
    @CsvIndex(7)
    Optional<String> loenn;
}
