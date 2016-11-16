package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static org.mockito.Mockito.verify;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class HazelcastBackendTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

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