package no.spk.pensjon.faktura.tidsserie.util;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceReference;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Hjelpemetoder for å jobbe med {@link ServiceRegistry}.
 */
public final class Services {
    private  static final String[] MATCH_ANY = new String[0];

    private Services(){
        // no instances
    }

    /**
     * Hjelpemetode som henter reigstrert tjeneste av type fra angitt tjenesteregister
     * @param registry tjenesteregister hvor tjenesten er registrert
     * @param type klassen til typen på tjenesten
     * @param <T> typen tjeneste
     * @return instans av angitt tjenestetype
     * @throws IllegalStateException dersom det ikke finnes en tjeneste av angitt type i registeret
     * @see ServiceRegistry#getServiceReference(Class)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public static <T> T lookup(ServiceRegistry registry, final Class<T> type) {
        return lookup(registry, type, MATCH_ANY);
    }

    /**
     * Hjelpemetode som henter reigstrert tjeneste av type fra angitt tjenesteregister og egenskap.
     * @param registry tjenesteregister hvor tjenesten er registrert
     * @param type klassen til typen på tjenesten
     * @param <T> typen tjeneste
     * @param egenskap på tjenesten
     * @return instans av angitt tjenestetype
     * @throws IllegalStateException dersom det ikke finnes en tjeneste av angitt type i registeret
     * @see ServiceRegistry#getServiceReference(Class, String...)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public static <T> T lookup(ServiceRegistry registry, final Class<T> type, final String... egenskap) {
        return new ServiceLocator(registry)
                .firstService(type, egenskap)
                .orElseThrow(() -> new IllegalStateException(
                                "Ingen teneste av type " +
                                        type.getSimpleName()
                                        + " er registrert i tenesteregisteret."
                        )
                );
    }

    /**
     * Hjelpemetode som henter alle reigstrert tjenester av type
     * @param registry tjenesteregister hvor tjenesten er registrert
     * @param type klassen til typen på tjenesten
     * @param <T> typen tjeneste
     * @return strøm av instanser av angitt tjenestetype
     * @throws IllegalStateException dersom det ikke finnes en tjeneste av angitt type i registeret
     * @see ServiceRegistry#getServiceReference(Class, String...)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public static  <T> Stream<T> lookupAll(ServiceRegistry registry, final Class<T> type) {
        return registry
                .getServiceReferences(type)
                .stream()
                .map(registry::getService)
                .flatMap(Optionals::stream);
    }

}
