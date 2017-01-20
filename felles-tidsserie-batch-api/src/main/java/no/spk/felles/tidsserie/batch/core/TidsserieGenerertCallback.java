package no.spk.felles.tidsserie.batch.core;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Extension-interface for tjenestetilbydere som ønsker å bli notifisert når generering av tidsserien
 * har fullført uten å feile.
 * <br>
 * Extensionpunktet for tidsserie generert garanterer at {@link #tidsserieGenerert(ServiceRegistry)} vil bli kalt for
 * alle registrerte tjenester når tidsserien her blitt lagret.
 * <br>
 * Dersom noen av tjenestene feiler, vil det bli kastet en exception etter at alle tjenestene er behandlet, og batchen vil feile.
 *
 * {@link TidsserieGenerertCallback} skiller seg fra {@link TidsserieLivssyklus}
 * @author Snorre E. Brekke - Computas
 */
@FunctionalInterface
public interface TidsserieGenerertCallback {
    /**
     * Notifiserer tjenesten om at tidsserien er ferdig generert.
     *
     * @param serviceRegistry tjenesteregister som kan benyttes for å finne andre tjenester
     */
    void tidsserieGenerert(ServiceRegistry serviceRegistry);
}
