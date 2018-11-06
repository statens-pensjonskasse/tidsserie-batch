package no.spk.felles.tidsserie.batch.core;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import no.spk.faktura.input.BatchId;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Extension-interface for tjenestetilbydere som ønsker å bli notifisert når generering av tidsserien
 * har fullført uten å feile.
 * <br>
 * Extensionpunktet for tidsserie generert garanterer at {@link #tidsserieGenerert(ServiceRegistry, Metadata)}
 * vil bli kalt for alle registrerte tjenester når tidsserien her blitt lagret.
 * <br>
 * Dersom noen av tjenestene feiler, vil det bli kastet en exception etter at alle tjenestene er behandlet,
 * og batchen vil feile.
 * <p>
 * {@link TidsserieGenerertCallback2} skiller seg fra {@link TidsserieLivssyklus} gjennom
 * at den kun blir notifisert dersom tidsserien har blitt generert uten at modusen kaster noen feil.
 * <p>
 * {@link TidsserieGenerertCallback2} skiller seg fra den eldre versjonen,
 * {@link TidsserieGenerertCallback}, ved at den inkluderer {@link Metadata} om kjøringen som
 * gikk bra.
 *
 * @since 1.1.0
 */
public interface TidsserieGenerertCallback2 {
    /**
     * Notifiserer tjenesten om at tidsserien er ferdig generert, inkludert metadata
     * som beskriv køyringas unike id og køyretid.
     *
     * @param serviceRegistry tjenesteregister som kan benyttes for å finne andre tjenester
     * @param metadata ymse metadata om køyringa
     */
    void tidsserieGenerert(final ServiceRegistry serviceRegistry, final Metadata metadata);

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
