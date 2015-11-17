package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class AvtaleProduktFraCsv {
    @CsvIndex(0)
    Optional<String> type;
    @CsvIndex(1)
    Optional<String> avtaleId;
    @CsvIndex(2)
    Optional<String> produkt;
    @CsvIndex(3)
    Optional<String> fraOgMedDato;
    @CsvIndex(4)
    Optional<String> tilOgMedDato;
    @CsvIndex(5)
    Optional<String> produktInfo;
    @CsvIndex(6)
    Optional<String> arbeidsgiverpremieProsent;
    @CsvIndex(7)
    Optional<String> medlemspremieProsent;
    @CsvIndex(8)
    Optional<String> administrasjonsgebyrProsent;
    @CsvIndex(9)
    Optional<String> arbeidsgiverpremieBeloep;
    @CsvIndex(10)
    Optional<String> medlemspremieBeloep;
    @CsvIndex(11)
    Optional<String> administrasjonsgebyrBeloep;
    @CsvIndex(value = 12, obligatorisk = false)
    Optional<String> risikoklasse;
}
