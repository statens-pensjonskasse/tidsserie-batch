package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Hjelpeklasse for å kjøre en oppgave etter en gitt tid.
 *
 * @author Snorre E. Brekke - Computas
 */
public final class TimeoutTaskrunner {
    private TimeoutTaskrunner(){
        //Util klasse
    }

    /**
     * Planlegger en oppgave som vil kjøre {@code timeoutCallback} etter {@code timeout}.
     *
     * @param timeout tid til timeoutCallback kalt
     * @param timeoutCallback callback som skal kjøres ved timeout
     * @return {@link ScheduledFuture} som representerer den planlagte oppgaven
     * @see ScheduledFuture
     */
    public static ScheduledFuture<?> startTimeout(Duration timeout, Runnable timeoutCallback) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread timeoutTaskrunner = new Thread(r, "TimeoutTaskrunner");
            timeoutTaskrunner.setDaemon(true);
            return timeoutTaskrunner;
        });
        return executor.schedule(timeoutCallback::run, timeout.toMillis(), MILLISECONDS);
    }
}
