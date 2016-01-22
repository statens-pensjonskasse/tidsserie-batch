package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.core.ObservasjonsEvent;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.LoggerFactory;

public class LmaxDisruptorPublisher implements StorageBackend, TidsserieLivssyklus {
    private final ExecutorService executor;
    private final FileTemplate fileTemplate;

    private Disruptor<ObservasjonsEvent> disruptor;
    private RingBuffer<ObservasjonsEvent> ringBuffer;
    private ObservasjonsConsumer consumer;

    public LmaxDisruptorPublisher(final ExecutorService executor, final FileTemplate fileTemplate) {
        this.executor = executor;
        this.fileTemplate = requireNonNull(fileTemplate, "fileTemplate er påkrevd, men var null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(final ServiceRegistry registry) {
        // The factory for the buffer
        final ObservasjonsEventFactory factory = new ObservasjonsEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024 * 16;

        disruptor = new Disruptor<>(factory, bufferSize, this.executor);

        consumer = new FileWriterObservasjonsConsumer(this.fileTemplate);
        disruptor.handleEventsWith(consumer);
        // Start the Disruptor, starts all threads running
        disruptor.start();

        ringBuffer = disruptor.getRingBuffer();
    }

    @Override
    public void stop(final ServiceRegistry registry) {
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
    public void lagre(final Consumer<ObservasjonsEvent> consumer) {
        final long sequence = ringBuffer.next();
        try {
            consumer.accept(
                    ringBuffer
                            .get(sequence)
                            .reset()
            );
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
