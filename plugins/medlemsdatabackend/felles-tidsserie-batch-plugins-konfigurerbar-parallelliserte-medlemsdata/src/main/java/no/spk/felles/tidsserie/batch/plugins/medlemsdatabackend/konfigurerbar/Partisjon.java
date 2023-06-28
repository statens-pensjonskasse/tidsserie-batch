package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;

import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DatalagringStrategi;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

class Partisjon {
    private static final String DELIMITER_ROW = "\n";
    private static final String DELIMITER_COLUMN = ";";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Semaphore lock = new Semaphore(1, true);

    private final LinkedHashMap<String, Medlemsdata> medlemsdata = new LinkedHashMap<>();

    private final Partisjonsnummer nummer;

    Partisjon(final Partisjonsnummer nummer) {
        this.nummer = requireNonNull(nummer, "nummer er påkrevd, men var null");
    }

    void put(final String key, final byte[] medlemsdata, final DatalagringStrategi datalagringStrategi) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeBehandleMedlemsdataIPartisjonException(nummer, e);
        }
        executor.submit(() -> {
            try {
                this.medlemsdata.merge(
                        key,
                        datalagringStrategi.medlemsdata(medlemsdata),
                        Medlemsdata::put
                );
            } finally {
                lock.release();
            }
        });
    }

    Optional<List<List<String>>> get(final String medlemsId) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        try {
            Optional<List<List<String>>> resultat = Optional
                    .ofNullable(medlemsdata.get(medlemsId))
                    .map(this::somMedlemsdata);
            return resultat;
        } finally {
            lock.release();
        }
    }

    void forEach(final BiConsumer<String, List<List<String>>> consumer) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        try {
            medlemsdata.forEach(
                    (key, bytes) -> consumer.accept(
                            key,
                            somMedlemsdata(bytes)
                    )
            );
        } finally {
            lock.release();
        }
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
        try {
            return medlemsdata.isEmpty();
        } finally {
            lock.release();
        }
    }

    int size() {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        try {
            return medlemsdata.size();
        } finally {
            lock.release();
        }
    }

    @Override
    public String toString() {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            throw new KlarteIkkeLeseMedlemsdataIPartisjonException(nummer, e);
        }
        try {
            return format(
                    "%s (%d medlemmar)",
                    nummer.toString(),
                    medlemsdata.keySet().size()
            );
        } finally {
            lock.release();
        }
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
                .toList();
    }

    void tøm() {
        medlemsdata.clear();
    }

    void stop() {
        executor.shutdownNow();
        lock.drainPermits();
    }
}
