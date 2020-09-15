package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static org.mockito.Mockito.verify;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class HazelcastBackendTest {
    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock
    private Server server;


    private HazelcastBackend backend;

    @Before
    public void _before() {
        backend = new HazelcastBackend(server);
    }

    @Test
    public void skal_stoppe_server_ved_stop_av_backend() {
        backend.stop(registry.registry());
        verify(server).stop();
    }
}