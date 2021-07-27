package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static no.spk.felles.tidsserie.batch.backend.hazelcast.ServiceRegistryRule.erAvType;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class HazelcastPluginTest {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final MockitoRule mockito = rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private TidsserieBatchArgumenter args;

    private final Plugin plugin = new HazelcastPlugin();

    @Test
    public void skal_vere_tilgjengelig_via_service_loader_APIen() {
        Plugin.registrerAlle(
                registry.registry(),
                ServiceLoader.load(Plugin.class)
        );

        registry
                .assertTenesterAvType(Plugin.class)
                .filteredOn(plugin -> erAvType(plugin, HazelcastPlugin.class))
                .hasSize(1);
    }

    @Test
    public void skal_registrere_hazelcast_backend_som_plugin() {
        willReturn(antallProsessorar(1)).given(args).antallProsessorar();
        registry.registrer(TidsserieBatchArgumenter.class, args);

        plugin.aktiver(registry.registry());

        registry
                .assertFirstService(MedlemsdataBackend.class)
                .isNotEmpty()
                .containsInstanceOf(HazelcastBackend.class);

        registry
                .assertTenesterAvType(TidsserieLivssyklus.class)
                .filteredOn(livssyklus -> erAvType(livssyklus, HazelcastBackend.class))
                .hasSize(1);
    }
}