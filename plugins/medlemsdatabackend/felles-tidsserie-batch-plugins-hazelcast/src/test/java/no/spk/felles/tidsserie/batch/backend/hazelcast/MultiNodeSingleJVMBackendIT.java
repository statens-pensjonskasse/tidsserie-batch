package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.ofNullable;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.spi.properties.GroupProperty;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiNodeSingleJVMBackendIT {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final StandardOutputAndError ignorerLogging = new StandardOutputAndError();

    private MultiNodeSingleJVMBackend server;

    @Before
    public void _before() {
        server = new MultiNodeSingleJVMBackend(
                registry.registry(),
                antallProsessorar(1)
        );
    }

    @After
    public void _after() {
        ofNullable(server).ifPresent(Server::stop);
    }

    @Test
    public void skal_starte_antall_instansar_angitt_ved_konstruksjon() {
        final int antallNoder = 2;
        server = new MultiNodeSingleJVMBackend(
                registry.registry(),
                antallProsessorar(antallNoder)
        );
        // Reduserer køyretida for testen med 5 sekund i forhold til standardinnstillingane
        server.setProperty(GroupProperty.WAIT_SECONDS_BEFORE_JOIN.getName(), "0");

        final HazelcastInstance instance = server.start();
        assertThat(instance.getCluster().getMembers())
                .as("antall noder i hazelcast-gridet")
                .hasSize(antallNoder);
    }

    @Test
    public void skal_starte_hazelcast_ved_start() {
        final HazelcastInstance instance = server.start();
        assertIsRunning(instance).isTrue();
    }

    @Test
    public void skal_avslutte_hazelcast_ved_stop() {
        final HazelcastInstance instance = server.start();

        final List<LifecycleEvent> events = new ArrayList<>();
        instance.getLifecycleService().addLifecycleListener(events::add);

        server.stop();

        assertThat(
                events
                        .stream()
                        .map(LifecycleEvent::getState)
                        .filter(LifecycleState.SHUTDOWN::equals)
                        .collect(Collectors.toSet())

        )
                .as("livssykluseventar etter kall til stop")
                .contains(LifecycleState.SHUTDOWN)
        ;

        assertIsRunning(instance)
                .as("køyrer hazelcast?")
                .isFalse();
    }

    @Test
    public void skal_ikkje_sette_opp_backup_partisjonar_for_medlemsdata_mapen() {
        final String navn = "medlemsdata";
        assertThat(
                server.start()
                        .getConfig()
                        .getMapConfig(navn)
                        .getTotalBackupCount()
        )
                .as("totalt antall backups pr innslag i %s-mapen for Hazelcast", navn)
                .isEqualTo(0);
    }

    @Test
    public void skal_benytte_binary_som_in_memory_format() {
        final String navn = "medlemsdata";
        assertThat(
                server.start()
                        .getConfig()
                        .getMapConfig(navn)
                        .getInMemoryFormat()
        )
                .as("in-memory-format for %s-mapen for Hazelcast", navn)
                .isEqualTo(InMemoryFormat.BINARY);
    }

    @Test
    public void skal_aldri_evicte_medlemsdata_hazelast() {
        final String navn = "medlemsdata";
        assertThat(
                server.start()
                        .getConfig()
                        .getMapConfig(navn)
                        .getEvictionPolicy()
        )
                .as("eviction policy for %s-mapen for Hazelcast", navn)
                .isEqualTo(EvictionPolicy.NONE);
    }

    private static AbstractBooleanAssert<?> assertIsRunning(final HazelcastInstance instance) {
        return assertThat(instance.getLifecycleService().isRunning());
    }
}