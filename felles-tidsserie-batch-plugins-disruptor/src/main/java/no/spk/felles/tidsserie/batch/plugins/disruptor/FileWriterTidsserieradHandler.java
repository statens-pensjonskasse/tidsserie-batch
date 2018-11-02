package no.spk.felles.tidsserie.batch.plugins.disruptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.lagring.Tidsserierad;

/**
 * Fil-basert implementasjon av {@link TidsserieradHandler}.
 * <br>
 * {@link Tidsserierad}ar som blir mottatt av {@link #onEvent(Tidsserierad, long, boolean)} blir lagra
 * til disk via ordinær, ubuffra {@link Writer}-basert blocking I/O.
 * <br>
 * Ved slutten av kvar batch og ved lukking av consumeren, blir eventane flusha til disk.
 *
 * @author Tarjei Skorgenes
 */
class FileWriterTidsserieradHandler implements TidsserieradHandler {
    private final FileTemplate template;

    private Map<String, FileWriter> writers = new HashMap<>();

    FileWriterTidsserieradHandler(final FileTemplate template) {
        this.template = template;
    }

    /**
     * Lukkar alle åpne filer som rader har blitt lagra til i løpet av tidsseriegenereringa.
     *
     * @throws IOException dersom lukke av ei eller fleire av dei åpne feilene feilar
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
     * i kombinasjon med {@link FileTemplate#createUniqueFile(long, String)}, regulerer output-fila eventen blir ruta til.
     * <br>
     * Eventar utan serienummer vil bli behandla som om serienummeret er <code>1</code>.
     *
     * @param event eventen som inneheld innholdet som skal lagrast og serienummeret den eventuelt tilhøyrer
     * @param sequence sekvensnummeret til eventen som blir prosessert
     * @param endOfBatch <code>true</code> dersom dette er siste event i ein batch, <code>false</code> viss det
     * vil bli sendt inn fleire eventar til consumeren straks metoda returnerer
     * @throws UncheckedIOException dersom skriving til disk eller åpning av nye filer feilar
     */
    @Override
    public void onEvent(final Tidsserierad event, final long sequence, final boolean endOfBatch) throws UncheckedIOException {
        final FileWriter writer = this.writers.computeIfAbsent(
                event.serienummer().orElse(1L) + event.filprefix,
                (k) -> newWriter(event.serienummer().orElse(1L), event.filprefix)
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

    protected FileWriter newWriter(final Long serienummer, final String filprefix) {
        final File file = template.createUniqueFile(serienummer, filprefix);
        try {
            return new FileWriter(file, false);
        } catch (IOException e) {
            throw new UncheckedIOException("Klarte ikkje åpne " + file + " for skriving", e);
        }
    }
}