package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ArbeidsgiverCsv {
    @CsvIndex(1)
    Optional<String> arbeidsgiverId;
    @CsvIndex(2)
    Optional<String> innmeldtDato;
    @CsvIndex(3)
    Optional<String> utmeldtDato;
}
