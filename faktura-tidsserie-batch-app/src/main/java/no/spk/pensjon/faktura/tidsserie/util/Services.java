package no.spk.pensjon.faktura.tidsserie.util;

import java.util.Properties;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tjenesteregister.ServiceReference;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Hjelpemetoder for å jobbe med {@link ServiceRegistry}.
 */
public final class Services {
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
    public static  <T> T lookup(ServiceRegistry registry, final Class<T> type) {
        return registry
                .getServiceReference(type)
                .flatMap(registry::getService)
                .orElseThrow(() -> new IllegalStateException(
                                "Ingen teneste av type " +
                                        type.getSimpleName()
                                        + " er registrert i tenesteregisteret."
                        )
                );
    }

    /**
     * Hjelpemetode som henter reigstrert tjeneste av type fra angitt tjenesteregister og egenskap.
     * @param registry tjenesteregister hvor tjenesten er registrert
     * @param type klassen til typen på tjenesten
     * @param <T> typen tjeneste
     * @param egenskap på tjenesten
     * @return instans av angitt tjenestetype
     * @throws IllegalStateException dersom det ikke finnes en tjeneste av angitt type i registeret
     * @see ServiceRegistry#getServiceReference(Class, String)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public static  <T> T lookup(ServiceRegistry registry, final Class<T> type, final String egenskap) {
        return registry
                .getServiceReference(type, egenskap)
                .flatMap(registry::getService)
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
     * @see ServiceRegistry#getServiceReference(Class, String)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public static  <T> Stream<T> lookupAll(ServiceRegistry registry, final Class<T> type) {
        return registry
                .getServiceReferences(type)
                .stream()
                .map(registry::getService)
                .flatMap(Optionals::stream);
    }

    /**
     * Registrerer {@code tjeneste} i tjenesteregisteret under typen {@code type} med ein eller fleire valgfrie {@code
     * egenskapar}.
     *
     * @param registry registeret tjenesten skal registreres hos
     * @param <T> tjenestetypen
     * @param type tjenestetypen tjenesta skal registrerast under i tjenesteregisteret
     * @param tjeneste tjenesteinstansen som skal registrerast
     * @param egenskapar 0 eller fleire egenskapar på formatet egenskap=verdi
     * @return registreringa for tjenesta
     */
    public static <T> ServiceRegistration<T> registrer(final ServiceRegistry registry, final Class<T> type, final T tjeneste, final String... egenskapar) {
        return registry.registerService(
                type,
                tjeneste,
                Stream.of(egenskapar)
                        .map(egenskap -> {
                            final int index = egenskap.indexOf("=");
                            return new String[]{egenskap.substring(0, index), egenskap.substring(index + 1)};
                        })
                        .reduce(
                                new Properties(),
                                (Properties props, String[] egenskap) -> {
                                    props.setProperty(egenskap[0], egenskap[1]);
                                    return props;
                                },
                                (a, b) -> {
                                    final Properties c = new Properties(a);
                                    c.putAll(b);
                                    return c;
                                }
                        )
        );
    }

}
