package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.premie.tidsserie.batch.core.registry.Ranking;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceReference;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ServiceRegistryExtension implements BeforeEachCallback {
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

    public <T> void registrer(final Class<T> tjenestetype, final T tjeneste, final String... egenskapar) {
        registry.registerService(tjenestetype, tjeneste, egenskapar);
    }

    public <T> Optional<T> firstService(final Class<T> type) {
        return locator.firstService(type);
    }

    public <T> Stream<T> allServices(final Class<T> tjenestetype) {
        return
                registry()
                        .getServiceReferences(tjenestetype)
                        .stream()
                        .map(registry()::getService)
                        .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty));
    }

    public <T> ServiceRegistryExtension assertFirstService(
            final Class<T> tjenestetype,
            final Consumer<ServiceAssert<T>> assertion
    ) {
        final Optional<ServiceReference<T>> reference = registry.getServiceReference(tjenestetype);
        assertThat(reference)
                .as("ServiceRegistry.getServiceReference(%s.class)", tjenestetype.getSimpleName())
                .isNotEmpty();
        assertion.accept(
                new ServiceAssert<>(
                        reference.get(),
                        firstService(tjenestetype)
                                .orElseThrow(NoSuchElementException::new)
                )
        );
        return this;
    }

    public <T> AbstractListAssert<?, List<? extends T>, T, ObjectAssert<T>> assertAllServices(final Class<T> tjenestetype) {
        return assertThat(
                allServices(tjenestetype)
        )
                .as("alle tjenester for tjenestetype " + tjenestetype.getSimpleName());
    }

    static class ServiceAssert<T> extends ObjectAssert<T> {
        private final ServiceReference<T> reference;

        ServiceAssert(final ServiceReference<T> reference, final T service) {
            super(service);
            this.reference = requireNonNull(reference, "reference er påkrevd, men var null");
        }

        public ServiceAssert<T> harRanking(final Ranking expected) {
            new RankingAssert(reference).erLik(expected);
            return this;
        }

        <T2> ObjectAssert<T2> som(final Class<T2> expected) {
            isInstanceOf(expected);
            return assertThat(expected.cast(actual));
        }
    }

    private static int somTall(final String egenskap) {
        return Integer.parseInt(egenskap.split("=")[1]);
    }

    private static class RankingAssert extends ObjectAssert<Ranking> {
        private RankingAssert(final ServiceReference<?> reference) {
            super(
                    reference
                            .getProperty("service.ranking")
                            .map(Integer::parseInt)
                            .map(Ranking::ranking)
                            .orElseThrow(NoSuchElementException::new)
            );
        }

        private void erLik(final Ranking that) {
            assertRanking().isEqualTo(somTall(that.egenskap()));
        }

        private AbstractIntegerAssert<?> assertRanking() {
            return assertThat(
                    somTall(actual.egenskap())
            )
                    .as(actual.egenskap());
        }
    }
}
