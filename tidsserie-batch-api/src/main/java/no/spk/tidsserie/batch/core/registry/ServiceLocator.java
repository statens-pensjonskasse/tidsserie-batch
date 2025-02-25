package no.spk.tidsserie.batch.core.registry;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Supplier;

import no.spk.pensjon.faktura.tjenesteregister.ServiceReference;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * ServiceLocator tilbyr oppslag av standardtjenesta for angitte tjenestetyper via tjenesteregisteret.
 *
 * @author Tarjei Skorgenes
 */
public class ServiceLocator {
    private final ServiceRegistry registry;

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

    /**
     * Slår opp den høgast rangerte tjenesta av type {@code tjenestetype} som ei obligatorisk tjeneste som
     * oppslaget skal feile på viss manglar.
     * <br>
     * Dersom eit eller fleire {@code filter} er angitt, blir dei brukt for å filtrere kva standardtjeneste som
     * blir henta ut.
     *
     * @param <T>          tjenestetypen
     * @param tjenestetype tjenestetypen for tjenesta som skal bli slått opp
     * @param filter       eit variabelt antall filter som kan benyttast for å filtrere bort uønska implementasjonar av
     *                     tjenestetypen før standardtjenesta blant dei gjennverande blir returnert
     * @return standardtjenesta av angitt type og som matchar angitte filter
     * @throws IllegalStateException dersom det ikkje er mulig å lokalisere ei tjeneste av den aktuelle typen og som matchar kvart filter
     * @see ServiceRegistry#getServiceReference(Class, String...)
     * @see ServiceRegistry#getService(ServiceReference)
     */
    public <T> T firstMandatory(final Class<T> tjenestetype, final String... filter) {
        return firstService(tjenestetype, filter)
                .orElseThrow(ukjentTjeneste(tjenestetype, filter));
    }

    /**
     * Feil for situasjonar der {@link #firstService(Class, String...)} ikkje klarer å lokalisere
     * ei tjeneste som klienten er avhengig av.
     *
     * @param type   tjenestetypen som oppslag av har feila
     * @param filter 0 eller fleire filter som den ukjente tjenesta skulle ha matcha
     * @return ein ny generator for feilmeldingar når tjenestetypen ikkje er tilgjengelig i tjenesteregisteret
     */
    public static Supplier<IllegalStateException> ukjentTjeneste(final Class<?> type, final String... filter) {
        return () -> new IllegalStateException(
                "Ingen teneste av type " +
                        type.getSimpleName()
                        + " er registrert i tenesteregisteret "
                        + " og matchar følgjande filter: " + asList(filter)
        );
    }
}
