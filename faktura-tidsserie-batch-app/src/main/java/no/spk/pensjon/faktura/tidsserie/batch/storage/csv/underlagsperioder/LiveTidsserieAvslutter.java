package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.batch.TidsserieResulat;

/**
 * Klasse som lager filer som benyttes av datavarehus ved innlesing av livetidsserien.
 * Filene opprettes etter tidsserien er ferdig generert.
 * @author Snorre E. Brekke - Computas
 */
public class LiveTidsserieAvslutter {
    private final TidsserieResulat resulat;

    public LiveTidsserieAvslutter(TidsserieResulat resulat) {
        this.resulat = requireNonNull(resulat, "resulat kan ikke være null.");
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     */
    public LiveTidsserieAvslutter lagCsvGruppefiler() {
        new CsvFileGroupWriter().createCsvGroupFiles(resulat.tidsserieKatalog());
        return this;
    }

    /**
     * Oppretter ok.trg i tidsseriekatalogen.
     */
    public LiveTidsserieAvslutter lagTriggerfil() {
        Path resolve = resulat.tidsserieKatalog().resolve("ok.trg");
        try {
            Files.createFile(resolve);
        } catch (IOException e) {
            throw new UncheckedIOException("Klarte ikke å opprette triggerfil.", e);
        }
        return this;
    }
}
