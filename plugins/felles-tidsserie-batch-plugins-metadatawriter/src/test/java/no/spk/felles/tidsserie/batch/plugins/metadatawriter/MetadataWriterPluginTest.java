package no.spk.felles.tidsserie.batch.plugins.metadatawriter;

import static no.spk.felles.tidsserie.batch.plugins.metadatawriter.ServiceRegistryRule.erAvType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;
import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MetadataWriterPluginTest {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolderWithDeleteVerification();

    private final Plugin plugin = new MetadataWriterPlugin();

    @Before
    public void _before() throws IOException {
        registry.registrer(
                Path.class,
                temp.newFolder("log").toPath(),
                Katalog.LOG.egenskap()
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
                .filteredOn(plugin -> erAvType(plugin, MetadataWriterPlugin.class))
                .hasSize(1);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void skal_registrere_metadatawriter_som_plugin() {
        registry.registrer(
                TidsserieBatchArgumenter.class,
                new ProgramArguments()
        );

        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(
                        no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback.class
                )
                .filteredOn(callback -> erAvType(callback, LagreMetadata.class))
                .isEmpty();
        registry
                .assertTenesterAvType(TidsserieGenerertCallback2.class)
                .filteredOn(callback -> erAvType(callback, LagreMetadata.class))
                .hasSize(1);
    }
}