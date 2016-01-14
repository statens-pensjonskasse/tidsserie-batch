package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.core.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TidsserieagentTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock
    private GenererTidsserieCommand command;

    @Mock
    private StorageBackend lagring;

    @Mock
    private Tidsseriemodus modus;

    private Map<String, Object> tenester = new HashMap<>();

    private Tidsserieagent agent = new Tidsserieagent();

    @Before
    public void _before() {
        registrer(tenester, GenererTidsserieCommand.class, command);
        registrer(tenester, StorageBackend.class, lagring);
        registrer(tenester, Tidsseriemodus.class, modus);
    }

    @Test
    public void skalFeilePaaOppslagAvTenester() {
        e.expect(IllegalStateException.class);
        e.expectMessage("Ingen teneste av type Object");
        e.expectMessage("- Integer");
        e.expectMessage("- String");

        final Map<String, Object> tenester = new HashMap<>();
        registrer(tenester, Integer.class, 1);
        registrer(tenester, String.class, "yada yada");

        Tidsserieagent.lookup(tenester, Object.class);
    }

    @Test
    public void skal_hente_tjenesteregisteret_fra_usercontexten() {
        final Map<String, Object> mock = spy(new HashMap<>());
        when(mock.get(ServiceRegistry.class.getSimpleName())).thenReturn(registry.registry());

        registry.registrer(GenererTidsserieCommand.class, command);
        registry.registrer(StorageBackend.class, lagring);
        registry.registrer(Tidsseriemodus.class, modus);

        agent.configure(mock);

        verify(mock).get(ServiceRegistry.class.getSimpleName());
    }

    private static <T> void registrer(final Map<String, T> tenester, Class<? extends T> type, T service) {
        tenester.put(type.getSimpleName(), service);
    }
}