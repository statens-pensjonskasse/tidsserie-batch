package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

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
    private final FileWriter writer;

    public FileWriterObservasjonsConsumer(FileTemplate fileTemplate) throws IOException {
        writer = newWriter(fileTemplate);
    }

    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void onEvent(ObservasjonsEvent event, long sequence, boolean endOfBatch) throws Exception {
        writer.write(event.buffer.toString());
        if (endOfBatch) {
            writer.flush();
        }
    }

    FileWriter newWriter(final FileTemplate template) throws IOException {
        return new FileWriter(template.createUniqueFile(), false);
    }
}