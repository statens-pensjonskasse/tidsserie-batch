package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
public class KundedataCsv {
    @CsvIndex(1)
    Optional<String> kundedataId;
    @CsvIndex(2)
    Optional<String> orgnummer;
    @CsvIndex(3)
    Optional<String> fraOgMedDato;
    @CsvIndex(4)
    Optional<String> tilOgMedDato;
    @CsvIndex(5)
    Optional<String> arbeidsgiverId;
}

