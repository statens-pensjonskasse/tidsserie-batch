package no.spk.pensjon.faktura.tidsserie.core;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Extensionpoint lar modular utanfor faktura-tidsserie-batch-app plugge inn tjenester for å overstyre eller legge til,
 * utvidelsar som utvidar eller endrar standardoppførselen til batchen.
 * <br>
 * Mekanisma er ei realisering av OCP-prinsippet som sikrar at framtidige utvidelsar og/eller endring av oppførsel
 * kan skje utan å måtte modifisere på nokon av klassene i standardplatforma for situasjonar der det er definert
 * eit extension point for utvidelsen som ønskast plugga inn.
 * <br>
 * Eit anna hovedmål med designet er å tilby ein meir dynamisk modell for samhandling med tjenester som er plugga inn.
 * Motstykket til dette er den meir statiske modellen som {@link ServiceLocator} støttar i form av at den lar klientane
 * slå opp og holde på referansar til tjenester dei vil samhandle med på eit seinare tidspunkt. Extensionpoint-mekanisma
 * legg derimot opp til at klientane ikkje skal trenge å holde på direkte referansar til tjenester dei samhandlar med
 * ettersom desse blir slått opp dynamisk ved kall til {@link #invokeAll(Consumer)} og {@link #invokeFirst(Consumer)}.
 * <br>
 * Ein siste sentral tanke med Extensionpoint-mekanisma er å gjere feilhandtering til ein sentral del av
 * standardplatformas oppførsel. Ved å fange alle {@link RuntimeException}s som oppstår ved kall ut av standardplatforma
 * og til tjenester plugga inn frå klientar og plugins, står platforma sjøl ansvarlig for å definere i kvar tjenestetype
 * som den kallar ut til, kva som er forventa oppførsel ved feil. Standardplatforma kan f.eks. velge å ignorere heilt,
 * ignorere men logge, rethrowe eller feile med ein annan feiltype enn opprinnelig, i etterkant av at alle innplugga
 * tjenester har blitt kalla. Dette i motsetning til at den feilar umiddelbart når første tjeneste feilar, utan å kalle
 * påfølgjande tjenester og dermed fråta dei muligheita til å agere på tjenestekallet som blir utført.
 * <br>
 * I første utgåve er Extensionpoint-mekanisma reindyrka i forhold til å kun støtte kommandoar, dvs operasjonar uten
 * returverdi. For tjenester som har returverdi må eventuelt {@link ServiceLocator} benyttast for å kommunisere
 * med tjenesta direkte.
 *
 * @param <T> tjenestetypen som extensionpointet støttar
 * @author Tarjei Skorgenes
 * @since 2.1.0
 */
public class Extensionpoint<T> {
    private final ServiceRegistry registry;
    private final Class<T> type;

    /**
     * Konstruerer eit nytt extensionpoint som støttar tjenester av angitt {@code type}
     * og som er registrert i tjenesteregisteret.
     *
     * @param type tjenestetypen som skal kunne bli kalla via extension pointet
     * @param registry tjenesteregisteret tjenestene vil bli slått opp via
     * @throws NullPointerException dersom nokon av argumenta er lik {@code null}
     */
    public Extensionpoint(final Class<T> type, final ServiceRegistry registry) {
        this.registry = requireNonNull(registry);
        this.type = requireNonNull(type);
    }

    /**
     * Kallar alle tjenester av typen extensionpointet støttar ved hjelp av {@code operation}.
     * <br>
     * Dersom ei eller fleire av tjenestene feilar blir feilen fanga og inkludert i det returnerte statusobjektet.
     * <br>
     * For klientar som ønskjer å inspisere resultatet av kallet til alle tjenestene, kan statusobjektet
     * inspiserast nærmare.
     *
     * @param operation operasjonen som skal kalle kvar tjeneste som blir henta opp
     * @return eit nytt objekt som inneheld status for operasjonen som har blitt utført på alle tjenestene
     */
    public ExtensionpointStatus invokeAll(final Consumer<T> operation) {
        return invokeSafely(
                operation,
                registry
                        .getServiceReferences(type)
                        .stream()
                        .map(registry::getService)
                        .flatMap(Extensionpoint::stream)
        );
    }

    /**
     * Kallar den høgast rangerte tjenesta av typen extensionpointet støttar ved hjelp av {@code operation}.
     * <br>
     * Dersom tjenesta feilar blir feilen fanga og inkludert i det returnerte statusobjektet.
     * <br>
     * For klientar som ønskjer å inspisere resultatet av kallet til tjenestea, kan statusobjektet
     * inspiserast nærmare.
     *
     * @param operation operasjonen som skal kalle den høgast rangerte tjenesta som blir henta opp
     * @return eit nytt objekt som inneheld status for operasjonen som har blitt utført på den høgast rangerte tjenesta
     */
    public ExtensionpointStatus invokeFirst(final Consumer<T> operation) {
        return invokeSafely(
                operation,
                stream(
                        registry
                                .getServiceReference(type)
                                .flatMap(registry::getService)
                )
        );
    }

    private ExtensionpointStatus invokeSafely(final Consumer<T> operation, final Stream<T> extensions) {
        return extensions
                .reduce(
                        ExtensionpointStatus.ok(),
                        (status, extension) -> status.invoke(operation, extension),
                        ExtensionpointStatus::merge
                );
    }

    private static <T> Stream<T> stream(final Optional<T> o) {
        return o.map(Stream::of).orElse(Stream.empty());
    }
}
