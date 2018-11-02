package no.spk.felles.tidsserie.batch.plugins.triggerfil;

import static no.spk.felles.tidsserie.batch.plugins.triggerfil.ServiceRegistryRule.erAvType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TriggerfilPluginTest {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private final Plugin plugin = new TriggerfilPlugin();

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
                .filteredOn(plugin -> erAvType(plugin, TriggerfilPlugin.class))
                .hasSize(1);
    }

    @Test
    public void skal_registrere_triggerfilecreator_som_plugin() {
        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(TidsserieGenerertCallback.class)
                .filteredOn(callback -> erAvType(callback, TriggerfileCreator.class))
                .hasSize(1);
    }
}