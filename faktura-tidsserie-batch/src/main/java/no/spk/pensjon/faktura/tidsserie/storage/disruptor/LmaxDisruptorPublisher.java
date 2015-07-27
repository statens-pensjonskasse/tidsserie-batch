package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.LoggerFactory;

public class LmaxDisruptorPublisher implements Closeable, StorageBackend {
    private final ExecutorService executor;
    private final FileTemplate fileTemplate;

    private Disruptor<ObservasjonsEvent> disruptor;
    private RingBuffer<ObservasjonsEvent> ringBuffer;
    private ObservasjonsConsumer consumer;

    public LmaxDisruptorPublisher(final ExecutorService executor, final FileTemplate fileTemplate) {
        this.executor = executor;
        this.fileTemplate = fileTemplate;
    }

    public void start() {
        // The factory for the buffer
        final ObservasjonsEventFactory factory = new ObservasjonsEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024 * 16;

        disruptor = new Disruptor<>(factory, bufferSize, this.executor);

        try {
            consumer = new FileWriterObservasjonsConsumer(this.fileTemplate);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        disruptor.handleEventsWith(consumer);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        ringBuffer = disruptor.getRingBuffer();
    }

    @Override
    public void close() {
        stop();
    }

    public void stop() {
        long i = 0;
        while (!ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize())) {
            try {
                if (i++ % 5000000 == 0) {
                    LoggerFactory.getLogger(getClass()).info("Waiting for the ringbuffer to be drained...");
                }
                Thread.sleep(0, 10000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        disruptor.shutdown();

        try {
            consumer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lagre(final Consumer<StringBuilder> consumer) {
        final long sequence = ringBuffer.next();
        try {
            final ObservasjonsEvent event = ringBuffer.get(sequence);
            final StringBuilder builder = event.buffer;
            builder.setLength(0);

            consumer.accept(builder);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
