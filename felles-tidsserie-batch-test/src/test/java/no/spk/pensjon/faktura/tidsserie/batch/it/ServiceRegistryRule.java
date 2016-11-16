package no.spk.pensjon.faktura.tidsserie.batch.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.ServiceLoader;

import no.spk.pensjon.faktura.tidsserie.batch.core.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.assertj.core.api.OptionalAssert;
import org.junit.rules.ExternalResource;

public class ServiceRegistryRule extends ExternalResource {
    private ServiceRegistry registry;
    private ServiceLocator locator;

    @Override
    protected void before() throws Throwable {
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
}
