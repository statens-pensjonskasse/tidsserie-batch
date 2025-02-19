package no.spk.premie.tidsserie.batch.plugins.triggerfil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Datavarehus har behov for at en trigger-fil med navn 'ok.trg' opprettes i tidsseriekatalogen
 * når batchen har kjørt ferdig. Når filen blir opprettet startes lasting av csv-filene som
 * er opprettet av batchen.
 * @author Snorre E. Brekke - Computas
 */
public class TriggerfileCreator implements TidsserieGenerertCallback2 {
    private final Path tidserieKatalog;

    TriggerfileCreator(Path tidserieKatalog) {
        this.tidserieKatalog = tidserieKatalog;
    }

    @Override
    public void tidsserieGenerert(final ServiceRegistry serviceRegistry, final Metadata metadata) {
        try {
            Files.createFile(tidserieKatalog.resolve("ok.trg"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
