package no.spk.pensjon.faktura.tidsserie.core;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import no.spk.pensjon.faktura.tjenesteregister.ServiceReference;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * ServiceLocator tilbyr oppslag av standardtjenesta for angitte tjenestetyper via tjenesteregisteret.
 *
 * @author Tarjei Skorgenes
 * @since 2.1.0
 */
public class ServiceLocator {
    private ServiceRegistry registry;

    /**
     * Konstruerer ein ny servicelocator som vil slå opp tjenester frå det angitte tjenesteregisteret.
     *
     * @param registry tjenesteregisteret som skal benyttast ved oppslag av tjenester via {@link #firstService(Class, String...)}
     * @throws NullPointerException dersom {@code registry} er {@code null}
     */
    public ServiceLocator(final ServiceRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    /**
     * Slår opp den høgast rangerte tjenesta av type {@code tjenestetype}.
     * <br>
     * Dersom eit eller fleire {@code filter} er angitt, blir dei brukt for å filtrere kva standardtjeneste som
     * blir henta ut.
     *
     * @param <T>          tjenestetypen
     * @param tjenestetype tjenestetypen for tjenesta som skal bli slått opp
     * @param filter       eit variabelt antall filter som kan benyttast for å filtrere bort uønska implementasjonar av
     *                     tjenestetypen før standardtjenesta blant dei gjennverande blir returnert
     * @return standardtjenesta av angitt type og som matchar angitte filter
     * @see ServiceRegistry#getServiceReference(Class, String...)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public <T> Optional<T> firstService(final Class<T> tjenestetype, final String... filter) {
        return registry
                .getServiceReference(tjenestetype, filter)
                .flatMap(registry::getService);
    }
}
