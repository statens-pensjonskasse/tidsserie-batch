package no.spk.pensjon.faktura.tidsserie.plugin.modus.underlagsperioder;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Klasse som lager filer som benyttes av datavarehus ved innlesing av livetidsserien.
 * Filene opprettes etter tidsserien er ferdig generert.
 * @author Snorre E. Brekke - Computas
 */
public class LiveTidsserieAvslutter implements TidsserieLivssyklus{
    private final Path tidserieKatalog;

    public LiveTidsserieAvslutter(Path tidserieKatalog) {
        this.tidserieKatalog = requireNonNull(tidserieKatalog, "tidserieKatalog kan ikke være null.");
    }

    @Override
    public void stop(ServiceRegistry serviceRegistry) {
        lagCsvGruppefiler().lagTriggerfil();
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     * @return this for chaning
     */
    public LiveTidsserieAvslutter lagCsvGruppefiler() {
        new CsvFileGroupWriter().createCsvGroupFiles(tidserieKatalog);
        return this;
    }

    /**
     * Oppretter ok.trg i tidsseriekatalogen.
     * @return this for chaning
     */
    public LiveTidsserieAvslutter lagTriggerfil() {
        Path resolve = tidserieKatalog.resolve("ok.trg");
        try {
            Files.createFile(resolve);
        } catch (IOException e) {
            throw new UncheckedIOException("Klarte ikke å opprette triggerfil.", e);
        }
        return this;
    }
}
