package no.spk.premie.tidsserie.batch.plugins.metadatawriter;

import static no.spk.premie.tidsserie.batch.plugins.metadatawriter.ServiceRegistryExtension.erAvType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.premie.tidsserie.batch.core.Katalog;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.premie.tidsserie.batch.core.registry.Plugin;
import no.spk.premie.tidsserie.batch.main.input.ProgramArguments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class MetadataWriterPluginTest {
    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    private final Plugin plugin = new MetadataWriterPlugin();

    @BeforeEach
    void _before(@TempDir File temp) throws IOException {
        registry.registrer(
                Path.class,
                newFolder(temp, "log").toPath(),
                Katalog.LOG.egenskap()
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
                .filteredOn(plugin -> erAvType(plugin, MetadataWriterPlugin.class))
                .hasSize(1);
    }

    @SuppressWarnings("deprecation")
    @Test
    void skal_registrere_metadatawriter_som_plugin() {
        registry.registrer(
                TidsserieBatchArgumenter.class,
                new ProgramArguments()
        );

        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(
                        TidsserieGenerertCallback.class
                )
                .filteredOn(callback -> erAvType(callback, LagreMetadata.class))
                .isEmpty();
        registry
                .assertTenesterAvType(TidsserieGenerertCallback2.class)
                .filteredOn(callback -> erAvType(callback, LagreMetadata.class))
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