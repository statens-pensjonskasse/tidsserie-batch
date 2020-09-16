package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator.ServiceRegistryRule.erAvType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ChecksumValidatorPluginTest {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolderWithDeleteVerification();

    private final Plugin plugin = new ChecksumValidatorPlugin();

    @Before
    public void _before() throws IOException {
        registry.registrer(
                Path.class,
                temp.newFolder("inn").toPath(),
                Katalog.GRUNNLAGSDATA.egenskap()
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
                .filteredOn(plugin -> erAvType(plugin, ChecksumValidatorPlugin.class))
                .hasSize(1);
    }

    @Test
    public void skal_registrere_standard_uttrekksvalidator_som_plugin() {
        plugin.aktiver(registry.registry());

        registry
                .assertTenesterAvType(UttrekksValidator.class)
                .filteredOn(callback -> erAvType(callback, ChecksumValideringAvGrunnlagsdata.class))
                .hasSize(1);
    }
}