package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DatalagringStrategi;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

class Partisjon {
    private static final String DELIMITER_ROW = "\n";
    private static final String DELIMITER_COLUMN = ";";

    private final ExecutorService threadpool = Executors.newFixedThreadPool(1);

    Semaphore lock = new Semaphore(1, true);

    private final LinkedHashMap<String, Medlemsdata> medlemsdata = new LinkedHashMap<>();

    private final Partisjonsnummer nummer;

    Partisjon(final Partisjonsnummer nummer) {
        this.nummer = requireNonNull(nummer, "nummer er påkrevd, men var null");
    }

    void put(final String key, final byte[] medlemsdata, DatalagringStrategi datalagringStrategi) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeBehandleMedlemsdataIPartisjonException(nummer, e);
        }
        threadpool.submit(() -> {
            this.medlemsdata.merge(
                    key,
                    datalagringStrategi.medlemsdata(medlemsdata),
                    Medlemsdata::put
            );
            lock.release();
        });
    }

    Optional<List<List<String>>> get(final String medlemsId) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        Optional<List<List<String>>> resultat = Optional
                .ofNullable(medlemsdata.get(medlemsId))
                .map(this::somMedlemsdata);
        lock.release();
        return resultat;
    }

    void forEach(final BiConsumer<String, List<List<String>>> consumer) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        medlemsdata.forEach(
                (key, bytes) -> consumer.accept(
                        key,
                        somMedlemsdata(bytes)
                )
        );
        lock.release();
    }

    Partisjonsnummer nummer() {
        return nummer;
    }

    boolean isEmpty() {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        boolean empty = medlemsdata.isEmpty();
        lock.release();
        return empty;
    }

    int size() {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        int size = medlemsdata.size();
        lock.release();
        return size;
    }

    @Override
    public String toString() {
        return format(
                "%s (%d medlemmar)",
                nummer.toString(),
                medlemsdata.keySet().size()
        );
    }

    private int ikkjeIgnorerTommeKolonnerPåSluttenAvRada() {
        return -1;
    }

    private List<List<String>> somMedlemsdata(final Medlemsdata medlemsdata) {
        return stream(
                new String(
                        medlemsdata.medlemsdata(),
                        StandardCharsets.UTF_8
                )
                        .split(DELIMITER_ROW)
        )
                .map(
                        row -> asList(
                                row.split(
                                        DELIMITER_COLUMN,
                                        ikkjeIgnorerTommeKolonnerPåSluttenAvRada()
                                )
                        )
                )
                .collect(toList());
    }

    void stop() {
        threadpool.shutdownNow();
        lock.drainPermits();
    }
}
