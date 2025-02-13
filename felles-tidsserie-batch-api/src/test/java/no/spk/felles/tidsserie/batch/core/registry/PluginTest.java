package no.spk.felles.tidsserie.batch.core.registry;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static no.spk.felles.tidsserie.batch.core.registry.Plugin.registrerAlle;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.atIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.ServiceRegistryExtension;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
public class PluginTest {

    @InjectSoftAssertions
    private SoftAssertions softly;



    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    @Mock
    private Plugin plugin_a;

    @Mock
    private Plugin plugin_b;

    @Test
    void skal_registrere_kvart_plugin_i_tjenesteregisteret() {
        registrerAlle(registry.registry(), asList(plugin_a, plugin_b));

        registry
                .assertTenesterAvType(Plugin.class)
                .hasSize(2)
                .containsOnly(plugin_a, plugin_b);
    }

    @Test
    void skal_registrere_feilande_plugin_i_tjenesteregisteret_men_utsette_kasting_av_feil_til_registrer_blir_kalla_seinare_i_oppstarten() {
        assertThatCode(
                () -> registrerAlle(
                        registry.registry(),
                        serviceLoader()
                                .thenReturn(plugin_a)
                                .thenThrow(new ServiceConfigurationError("Horribelt feilkonfigurert"))
                                .thenThrow(new NullPointerException("Null syns ikkje den er tull"))
                                .thenReturn(plugin_b)
                )
        )
                .doesNotThrowAnyException();

        final Set<Plugin> pluginUtenFeil = Stream.of(plugin_a, plugin_b).collect(toSet());
        registry.assertTenesterAvType(Plugin.class)
                .hasSize(4)
                .containsAll(pluginUtenFeil);

        assertRegistrerForFeilandePlugin(feilandePlugins(pluginUtenFeil), atIndex(0))
                .isInstanceOf(ServiceConfigurationError.class)
                .hasMessage("Horribelt feilkonfigurert");

        assertRegistrerForFeilandePlugin(feilandePlugins(pluginUtenFeil), atIndex(1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Null syns ikkje den er tull");
    }

    private List<Plugin> feilandePlugins(final Set<Plugin> pluginUtenFeil) {
        return registry.allServices(Plugin.class)
                .filter(fjern(pluginUtenFeil))
                .collect(Collectors.toList());
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertRegistrerForFeilandePlugin(final List<Plugin> pluginMedFeil, final Index index) {
        return softly.assertThatCode(
                () -> pluginMedFeil.get(index.value).aktiver(registry.registry())
        )
                .as(
                        "feilande plugin nr %d\nAlle plugins som feila:\n%s",
                        index.value + 1,
                        pluginMedFeil
                                .stream()
                                .map(plugin -> "- " + plugin)
                                .collect(joining("\n"))
                );
    }

    private Predicate<Plugin> fjern(final Set<Plugin> pluginUtenFeil) {
        return plugin -> !pluginUtenFeil.contains(plugin);
    }

    private FakeServiceLoader serviceLoader() {
        return new FakeServiceLoader();
    }

    private static class FakeServiceLoader implements Iterable<Plugin>, Iterator<Plugin> {
        private final ArrayList<Object> plugins = new ArrayList<>();

        private Iterator<Object> i;

        FakeServiceLoader thenReturn(final Plugin plugin) {
            plugins.add(plugin);
            return this;
        }

        FakeServiceLoader thenThrow(final Error e) {
            plugins.add(e);
            return this;
        }

        FakeServiceLoader thenThrow(final RuntimeException e) {
            plugins.add(e);
            return this;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<Plugin> iterator() {
            i = plugins.iterator();
            return this;
        }

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public Plugin next() {
            Object value = i.next();
            if (value instanceof Error) {
                throw (Error) value;
            }
            if (value instanceof RuntimeException) {
                throw (RuntimeException) value;
            }
            return (Plugin) value;
        }
    }
}