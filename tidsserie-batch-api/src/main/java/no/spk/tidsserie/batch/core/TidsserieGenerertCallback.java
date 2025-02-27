package no.spk.tidsserie.batch.core;

import no.spk.tidsserie.tjenesteregister.ServiceRegistry;

/**
 * @see TidsserieGenerertCallback2
 */
@Deprecated
public interface TidsserieGenerertCallback {
    /**
     * Notifiserer tjenesten om at tidsserien er ferdig generert.
     *
     * @param serviceRegistry tjenesteregister som kan benyttes for å finne andre tjenester
     */
    void tidsserieGenerert(ServiceRegistry serviceRegistry);
}
