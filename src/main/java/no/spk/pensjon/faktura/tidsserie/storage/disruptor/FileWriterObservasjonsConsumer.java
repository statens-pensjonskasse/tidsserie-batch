package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

class FileWriterObservasjonsConsumer implements ObservasjonsConsumer {
    private final FileWriter writer;

    public FileWriterObservasjonsConsumer(File file) throws IOException {
        writer = new FileWriter(new File(file.getParentFile(), file.getName().replace("XXX", UUID.randomUUID().toString())), true);
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