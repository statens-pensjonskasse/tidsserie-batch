package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class HazelcastBackendTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private Server server;

    private HazelcastBackend backend;

    @Before
    public void _before() {
        backend = new HazelcastBackend(server);
    }

    @Test
    public void skalRegistrereTenesterMedBackendServer() {
        final Object expected = new Object();
        backend.registrer(Object.class, expected);

        verify(server).registrer(Object.class, expected);
    }
}