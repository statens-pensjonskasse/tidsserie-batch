package no.spk.felles.tidsserie.batch.plugins.disruptor.gzipped;

import static no.spk.felles.tidsserie.batch.plugins.disruptor.gzipped.ServiceRegistryRule.erAvType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LmaxDisruptorPluginTest {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private final Plugin plugin = new LmaxDisruptorPlugin();

    @Before
    public void _before() throws IOException {
        registry.registrer(
                Path.class,
                temp.newFolder("ut").toPath(),
                Katalog.UT.egenskap()
        );
    }

    @Test
    public void skal_vere_tilgjengelig_via_service_loader_APIen() {
        Plugin.registrerAlle(
                registry.registry(),
                ServiceLoader.load(Plugin.class)
        );

        registry
                .assertTenesterAvType(Plugin.class)
                .filteredOn(plugin -> erAvType(plugin, LmaxDisruptorPlugin.class))
                .hasSize(1);
    }

    @Test
    public void skal_registrere_disruptor_backend_som_plugin() {
        plugin.aktiver(registry.registry());

        registry
                .assertFirstService(StorageBackend.class)
                .isNotEmpty()
                .containsInstanceOf(LmaxDisruptorPublisher.class);

        registry
                .assertTenesterAvType(TidsserieLivssyklus.class)
                .filteredOn(livssyklus -> erAvType(livssyklus, LmaxDisruptorPublisher.class))
                .hasSize(1);

        registry
                .assertFirstService(ExecutorService.class)
                .isNotEmpty();
    }
}