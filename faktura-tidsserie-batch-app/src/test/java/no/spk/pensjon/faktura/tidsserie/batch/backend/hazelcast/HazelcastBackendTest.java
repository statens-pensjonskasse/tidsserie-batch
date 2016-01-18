package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class HazelcastBackendTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private Server server;

    @Mock
    private Tidsseriemodus parameter;

    private HazelcastBackend backend;

    @Before
    public void _before() {
        backend = new HazelcastBackend(server);
    }
}