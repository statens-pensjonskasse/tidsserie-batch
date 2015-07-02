package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
class AvtaleversjonCsv {
    @CsvIndex(1)
    Optional<String> avtaleid;
    @CsvIndex(2)
    Optional<String> fraOgMedDato;
    @CsvIndex(3)
    Optional<String> tilOgMedDato;
    @CsvIndex(5)
    Optional<String> premiestatus;
}
