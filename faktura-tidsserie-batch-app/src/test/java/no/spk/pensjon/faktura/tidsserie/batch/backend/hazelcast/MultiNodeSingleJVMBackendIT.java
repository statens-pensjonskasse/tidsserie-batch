package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.instance.GroupProperties;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiNodeSingleJVMBackendIT {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();
    private MultiNodeSingleJVMBackend server;

    @Before
    public void _before() {
        server = new MultiNodeSingleJVMBackend(
                registry.registry(),
                1
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
                antallNoder
        );
        // Reduserer køyretida for testen med 5 sekund i forhold til standardinnstillingane
        server.setProperty(GroupProperties.PROP_WAIT_SECONDS_BEFORE_JOIN, "0");

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

    private static AbstractBooleanAssert<?> assertIsRunning(final HazelcastInstance instance) {
        return assertThat(instance.getLifecycleService().isRunning());
    }
}