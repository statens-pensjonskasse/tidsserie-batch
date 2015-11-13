package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;

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

    @Mock
    private TidsserieFactory grunnlagsdata;

    @Mock
    private StorageBackend lagring;

    @Mock
    private Tidsseriemodus modus;

    private Map<String, Object> tenester = new HashMap<>();

    private Tidsserieagent agent = new Tidsserieagent(dato("1970.01.01"), dato("1970.12.31"));

    @Before
    public void _before() {
        registrer(tenester, TidsserieFactory.class, grunnlagsdata);
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
    public void skalSlaaOppPaakrevdeTenester() {
        final Map<String, Object> mock = spy(new HashMap<>());
        when(mock.get(TidsserieFactory.class.getSimpleName())).thenReturn(grunnlagsdata);
        when(mock.get(StorageBackend.class.getSimpleName())).thenReturn(lagring);
        when(mock.get(Tidsseriemodus.class.getSimpleName())).thenReturn(modus);

        agent.configure(mock);

        verify(mock).get(TidsserieFactory.class.getSimpleName());
        verify(mock).get(StorageBackend.class.getSimpleName());
        verify(mock).get(Tidsseriemodus.class.getSimpleName());
    }

    private static <T> void registrer(final Map<String, T> tenester, Class<? extends T> type, T service) {
        tenester.put(type.getSimpleName(), service);
    }
}