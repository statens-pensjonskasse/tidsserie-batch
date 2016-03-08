package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Datavarehus har behov for at en trigger-fil med navn 'ok.trg' opprettes i tidsseriekatalogen
 * når batchen har kjørt ferdig. Når filen blir opprettet startes lasting av csv-filene som
 * er opprettet av batchen.
 * @author Snorre E. Brekke - Computas
 */
public class TriggerfileCreator implements TidsserieGenerertCallback {
    private final Path tidserieKatalog;

    public TriggerfileCreator(Path tidserieKatalog) {
        this.tidserieKatalog = tidserieKatalog;
    }

    @Override
    public void tidsserieGenerert(ServiceRegistry serviceRegistry) {
        try {
            Files.createFile(tidserieKatalog.resolve("ok.trg"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
