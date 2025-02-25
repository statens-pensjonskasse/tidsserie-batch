package no.spk.tidsserie.batch.plugins.disruptor;

import static no.spk.tidsserie.batch.plugins.disruptor.ServiceRegistryExtension.erAvType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

import no.spk.tidsserie.batch.core.Katalog;
import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.tidsserie.batch.core.registry.Plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class LmaxDisruptorPluginTest {

    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    @TempDir
    public File temp;

    private final Plugin plugin = new LmaxDisruptorPlugin();

    @BeforeEach
    void _before() throws IOException {
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
                .filteredOn(plugin -> erAvType(plugin, LmaxDisruptorPlugin.class))
                .hasSize(1);
    }

    @Test
    void skal_registrere_disruptor_backend_som_plugin() {
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

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}