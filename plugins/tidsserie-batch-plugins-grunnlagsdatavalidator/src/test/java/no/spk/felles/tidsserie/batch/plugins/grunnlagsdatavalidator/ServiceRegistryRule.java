package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.assertj.core.api.ListAssert;
import org.assertj.core.api.OptionalAssert;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
public class ServiceRegistryRule implements BeforeEachCallback {
    private ServiceRegistry registry;
    private ServiceLocator locator;

    @Override
    public void beforeEach(ExtensionContext context) {
        registry = ServiceLoader.load(ServiceRegistry.class).iterator().next();
        locator = new ServiceLocator(registry);
    }

    public ServiceRegistry registry() {
        return registry;
    }

    public <T> ServiceRegistration<T> registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        return registry.registerService(type, tjeneste, egenskapar);
    }

    public <T> Optional<T> firstService(final Class<T> type) {
        return locator.firstService(type);
    }

    public <T> OptionalAssert<T> assertFirstService(final Class<T> type) {
        return assertThat(firstService(type)).as("standardteneste for tenestetype " + type.getSimpleName());
    }

    public <T> Stream<T> allServices(final Class<T> type) {
        return registry.getServiceReferences(type)
                .stream()
                .flatMap(
                        reference -> registry
                                .getService(reference)
                                .map(Stream::of)
                                .orElseGet(Stream::empty)
                );
    }

    public <T> ListAssert<T> assertTenesterAvType(final Class<T> type) {
        return assertThat(allServices(type))
                .as("registrerte tenester av type %s", type.getSimpleName());
    }

    public static <T, I extends T> boolean erAvType(final T teneste, final Class<I> implementasjon) {
        return teneste.getClass().isAssignableFrom(implementasjon);
    }
}
