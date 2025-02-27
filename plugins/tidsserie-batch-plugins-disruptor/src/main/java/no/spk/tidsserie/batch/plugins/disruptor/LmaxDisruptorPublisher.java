package no.spk.tidsserie.batch.plugins.disruptor;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.tidsserie.batch.core.lagring.Tidsserierad;
import no.spk.tidsserie.tjenesteregister.ServiceRegistry;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.LoggerFactory;

public class LmaxDisruptorPublisher implements StorageBackend, TidsserieLivssyklus {
    private final ThreadFactory threadFactory;
    private final FileTemplate fileTemplate;

    private Disruptor<Tidsserierad> disruptor;
    private RingBuffer<Tidsserierad> ringBuffer;
    private TidsserieradHandler consumer;

    public LmaxDisruptorPublisher(final ThreadFactory threadFactory, final FileTemplate fileTemplate) {
        this.threadFactory = threadFactory;
        this.fileTemplate = requireNonNull(fileTemplate, "fileTemplate er påkrevd, men var null");
    }

    @Override
    public void start(final ServiceRegistry registry) {
        // The factory for the buffer
        final TidsserieradFactory factory = new TidsserieradFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024 * 16;

        disruptor = new Disruptor<>(factory, bufferSize, this.threadFactory);

        consumer = new FileWriterTidsserieradHandler(this.fileTemplate);
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
    public void lagre(final Consumer<Tidsserierad> consumer) {
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
