package no.spk.tidsserie.batch.plugins.triggerfil;

import static no.spk.tidsserie.batch.plugins.triggerfil.ServiceRegistryExtension.erAvType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.tidsserie.batch.core.Katalog;
import no.spk.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.tidsserie.batch.core.registry.Plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class TriggerfilPluginTest {
    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();


    private final Plugin plugin = new TriggerfilPlugin();

    @BeforeEach
    void _before(@TempDir File temp) throws IOException {
        registry.registrer(
                Path.class,
                newFolder(temp, "ut").toPath(),
                Katalog.UT.egenskap()
        );
    }

    @Test
    void skal_vere_tilgjengelig_via_service_loader_APIen() {
        Plugin.registrerAlle(
                registry.registry(),
                ServiceLoader.load(Plugin.class)
        );

        registry
                .assertTenesterAvType(Plugin.class)
                .filteredOn(plugin -> erAvType(plugin, TriggerfilPlugin.class))
                .hasSize(1);
    }

    @SuppressWarnings("deprecation")
    @Test
    void skal_registrere_triggerfilecreator_som_plugin() {
        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(
                        TidsserieGenerertCallback.class
                )
                .filteredOn(callback -> erAvType(callback, TriggerfileCreator.class))
                .isEmpty();

        registry
                .assertTenesterAvType(TidsserieGenerertCallback2.class)
                .filteredOn(callback -> erAvType(callback, TriggerfileCreator.class))
                .hasSize(1);
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}