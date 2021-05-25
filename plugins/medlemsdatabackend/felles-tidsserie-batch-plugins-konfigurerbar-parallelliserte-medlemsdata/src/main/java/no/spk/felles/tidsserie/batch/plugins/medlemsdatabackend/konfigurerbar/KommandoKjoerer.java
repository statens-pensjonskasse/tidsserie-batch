package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

interface KommandoKjoerer<T> extends AutoCloseable {
    Future<T> start(Callable<T> task);

    @Override
    void close();

    static <T> KommandoKjoerer<T> velgFlertrådskjøring(final int antallTråder) {
        return new FlertraadsKjoerer<>(antallTråder);
    }

    class SynkronKjoerer<T> implements KommandoKjoerer<T> {
        @Override
        public Future<T> start(final Callable<T> task) {
            return new Future<T>() {
                @Override
                public boolean cancel(final boolean mayInterruptIfRunning) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isCancelled() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isDone() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public T get() throws ExecutionException {
                    try {
                        return task.call();
                    } catch (final Exception | Error e) {
                        throw new ExecutionException(e);
                    }
                }

                @Override
                public T get(final long timeout, final TimeUnit unit) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public void close() {
        }
    }

    class FlertraadsKjoerer<T> implements KommandoKjoerer<T> {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final ExecutorService executor;

        public FlertraadsKjoerer(final int antallTråder) {
            this.executor = Executors.newFixedThreadPool(
                    antallTråder,
                    r -> new Thread(r, "pa-res-ba-01-" + threadNumber.getAndAdd(1))
            );
        }

        @Override
        public Future<T> start(final Callable<T> task) {
            return executor.submit(task);
        }

        @Override
        public void close() {
            executor.shutdown();
        }
    }

    /**
     * For bruk i tester som har behov for å fange inn alle oppgaver som blir forsøkt eksekvert.
     */
    class Spion<T> implements KommandoKjoerer<T> {
        private final Set<Callable<T>> tasks = new HashSet<>();

        @Override
        public Future<T> start(final Callable<T> task) {
            tasks.add(task);
            return new Stub<>();
        }

        @Override
        public void close() {
        }

        public Set<Callable<T>> tasks() {
            return Collections.unmodifiableSet(tasks);
        }

        private static class Stub<T> implements Future<T> {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isCancelled() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDone() {
                throw new UnsupportedOperationException();
            }

            @Override
            public T get() {
                throw new UnsupportedOperationException();
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
