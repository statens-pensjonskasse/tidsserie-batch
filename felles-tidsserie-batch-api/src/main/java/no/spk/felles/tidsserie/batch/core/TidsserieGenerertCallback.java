package no.spk.felles.tidsserie.batch.core;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import no.spk.faktura.input.BatchId;
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
 *
 * @author Snorre E. Brekke - Computas
 */
public interface TidsserieGenerertCallback {
    /**
     * Notifiserer tjenesten om at tidsserien er ferdig generert.
     *
     * @param serviceRegistry tjenesteregister som kan benyttes for å finne andre tjenester
     */
    @Deprecated
    default void tidsserieGenerert(ServiceRegistry serviceRegistry) {
    }

    /**
     * Notifiserer tjenesten om at tidsserien er ferdig generert.
     * <p>
     * For bakoverkompatibilitet, for klientar som ikkje treng <code>metadata</code> delegerer denne metoda til
     * {@link #tidsserieGenerert(ServiceRegistry)} som default.
     *
     * @param serviceRegistry tjenesteregister som kan benyttes for å finne andre tjenester
     * @param metadata ymse metadata om køyringa
     * @since 1.1.0
     */
    @SuppressWarnings("deprecation")
    default void tidsserieGenerert(final ServiceRegistry serviceRegistry, final Metadata metadata) {
        tidsserieGenerert(serviceRegistry);
    }

    static Metadata metadata(BatchId kjøring, Duration kjøretid) {
        return new Metadata(kjøring, kjøretid);
    }

    /**
     * Metadata relatert til/om kjøringen.
     *
     * @since 1.1.0
     */
    class Metadata {
        public final BatchId kjøring;
        public final Duration kjøretid;

        private Metadata(final BatchId kjøring, final Duration kjøretid) {
            this.kjøring = requireNonNull(kjøring, "kjøring er påkrevd, men var null");
            this.kjøretid = requireNonNull(kjøretid, "kjøretid er påkrevd, men var null");
        }
    }
}
