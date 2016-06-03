package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Klasse som lager filer som benyttes av datavarehus ved innlesing av livetidsserien.
 * Filene opprettes etter tidsserien er ferdig generert.
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagAvslutter implements TidsserieGenerertCallback {
    private final Path tidserieKatalog;

    public AvtaleunderlagAvslutter(Path tidserieKatalog) {
        this.tidserieKatalog = requireNonNull(tidserieKatalog, "tidserieKatalog kan ikke være null.");
    }


    @Override
    public void tidsserieGenerert(ServiceRegistry serviceRegistry) {
        lagCsvGruppefiler();
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     * @return this for chaning
     */
    public AvtaleunderlagAvslutter lagCsvGruppefiler() {
        new CsvFileGroupWriter().createCsvGroupFiles(tidserieKatalog);
        return this;
    }
}
