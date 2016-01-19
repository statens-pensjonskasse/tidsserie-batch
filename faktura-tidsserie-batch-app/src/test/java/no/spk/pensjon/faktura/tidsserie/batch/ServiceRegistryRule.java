package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.ServiceLoader;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistration;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.junit.rules.ExternalResource;

public class ServiceRegistryRule extends ExternalResource {
    private ServiceRegistry registry;

    @Override
    protected void before() throws Throwable {
        registry = ServiceLoader.load(ServiceRegistry.class).iterator().next();
    }

    public ServiceRegistry registry() {
        return registry;
    }

    public <T> ServiceRegistration<T> registrer(final Class<T> type, final T tjeneste, final String... egenskapar) {
        return registry.registerService(type, tjeneste, egenskapar);
    }
}
