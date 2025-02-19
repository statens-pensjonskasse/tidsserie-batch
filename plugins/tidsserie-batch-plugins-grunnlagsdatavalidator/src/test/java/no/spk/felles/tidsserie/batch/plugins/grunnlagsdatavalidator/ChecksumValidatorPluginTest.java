package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator.ServiceRegistryRule.erAvType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class ChecksumValidatorPluginTest {

    @RegisterExtension
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    private final Plugin plugin = new ChecksumValidatorPlugin();

    @BeforeEach
    void _before(@TempDir File temp) throws IOException {
        registry.registrer(
                Path.class,
                newFolder(temp, "inn").toPath(),
                Katalog.GRUNNLAGSDATA.egenskap()
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
                .filteredOn(plugin -> erAvType(plugin, ChecksumValidatorPlugin.class))
                .hasSize(1);
    }

    @Test
    void skal_registrere_standard_uttrekksvalidator_som_plugin() {
        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(UttrekksValidator.class)
                .filteredOn(callback -> erAvType(callback, ChecksumValideringAvGrunnlagsdata.class))
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