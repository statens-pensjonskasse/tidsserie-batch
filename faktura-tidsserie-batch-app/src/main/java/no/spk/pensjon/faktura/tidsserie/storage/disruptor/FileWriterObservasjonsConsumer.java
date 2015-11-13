package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;

/**
 * Fil-basert implementasjon av {@link ObservasjonsConsumer}.
 * <br>
 * {@link ObservasjonsEvent}ar som blir mottatt av {@link #onEvent(ObservasjonsEvent, long, boolean)} blir lagra
 * til disk via ordinær, ubuffra {@link Writer}-basert blocking I/O.
 * <br>
 * Ved slutten av kvar batch og ved lukking av consumeren, blir eventane flusha til disk.
 *
 * @author Tarjei Skorgenes
 */
class FileWriterObservasjonsConsumer implements ObservasjonsConsumer {
    private final FileTemplate template;

    private Map<Long, FileWriter> writers = new HashMap<>();

    FileWriterObservasjonsConsumer(final FileTemplate template) {
        this.template = template;
    }

    /**
     * Flushar og lukkar alle åpne filer
     *
     * @throws IOException
     */
    public void close() throws IOException {
        final List<IOException> feilVedLukking = new ArrayList<>();
        writers.forEach((serie, writer) -> close(writer, feilVedLukking));
        writers.clear();

        if (!feilVedLukking.isEmpty()) {
            final IOException e = new IOException("Lukking av " + feilVedLukking.size() + " CSV-filer feila.");
            feilVedLukking.forEach(e::addSuppressed);
            throw e;
        }
    }

    private void close(FileWriter writer, List<IOException> feilVedLukking) {
        try {
            writer.close();
        } catch (final IOException e) {
            feilVedLukking.add(e);
        }
    }

    /**
     * Lagrar unna det tekstlige innhaldet til eventen til flate filer ved hjelp av ordinær blocking I/O.
     * <br>
     * Innhaldet blir lagra til disk med systemet sin standard encoding.
     * <br>
     * Klienten kan styre kva filer eventen blir skreven til ved å sjå til at kvar event har eit serienummer som
     * i kombinasjon med {@link FileTemplate#createUniqueFile(long)}, regulerer output-fila eventen blir ruta til.
     * <br>
     * Eventar utan serienummer vil bli behandla som om serienummeret er <code>1</code>.
     *
     * @param event      eventen som inneheld innholdet som skal lagrast og serienummeret den eventuelt tilhøyrer
     * @param sequence   sekvensnummeret til eventen som blir prosessert
     * @param endOfBatch <code>true</code> dersom dette er siste event i ein batch, <code>false</code> viss det
     *                   vil bli sendt inn fleire eventar til consumeren straks metoda returnerer
     * @throws UncheckedIOException dersom skriving til disk eller åpning av nye filer feilar
     */
    @Override
    public void onEvent(final ObservasjonsEvent event, final long sequence, final boolean endOfBatch) throws UncheckedIOException {
        final FileWriter writer = this.writers.computeIfAbsent(
                event.serienummer().orElse(1L),
                this::newWriter
        );
        try {
            writer.write(event.buffer.toString());
            if (endOfBatch) {
                writer.flush();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(
                    "Lagring av observasjonsevent feila: " + e.getMessage() + "\n"
                            + "Event-serienummer: " + event.serienummer() + "\n"
                            + "Event-innhold: '" + event.buffer + "'",
                    e
            );
        }
    }

    protected FileWriter newWriter(final Long serienummer) {
        final File file = template.createUniqueFile(serienummer);
        try {
            return new FileWriter(file, false);
        } catch (IOException e) {
            throw new UncheckedIOException("Klarte ikkje åpne " + file + " for skriving", e);
        }
    }
}