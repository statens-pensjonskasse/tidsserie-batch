package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.FileWriter;
import java.io.IOException;

import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;

class FileWriterObservasjonsConsumer implements ObservasjonsConsumer {
    private final FileWriter writer;

    public FileWriterObservasjonsConsumer(FileTemplate fileTemplate) throws IOException {
        writer = new FileWriter(fileTemplate.createUniqueFile(), true);
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
}