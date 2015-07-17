package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class StillingsendringCsv {
    @CsvIndex(1)
    Optional<String> foedselsdato;
    @CsvIndex(2)
    Optional<String> personnummer;
    @CsvIndex(3)
    Optional<String> stillingsforhold;
    @CsvIndex(4)
    Optional<String> aksjonskode;
    @CsvIndex(8)
    Optional<String> stillingsprosent;
    @CsvIndex(9)
    Optional<String> loennstrinn;
    @CsvIndex(10)
    Optional<String> loenn;
    @CsvIndex(11)
    Optional<String> fasteTillegg;
    @CsvIndex(12)
    Optional<String> variableTillegg;
    @CsvIndex(13)
    Optional<String> funksjonstillegg;
    @CsvIndex(14)
    Optional<String> aksjonsdato;
    @CsvIndex(15)
    Optional<String> stillingskode;
    @CsvIndex(value = 16,obligatorisk = false)
    Optional<String> linjenummer;
}
