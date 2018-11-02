package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import java.util.HashMap;
import java.util.Map;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.ServiceRegistryRule;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.PartisjonsListener;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class TidsserieagentTest {
    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

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

    @SuppressWarnings("deprecation")
    @Mock
    private com.hazelcast.mapreduce.Context<String, Integer> context;

    private Tidsserieagent agent = new Tidsserieagent();

    @Before
    public void _before() {
        registry.registrer(GenererTidsserieCommand.class, command);
        registry.registrer(StorageBackend.class, lagring);
        registry.registrer(Tidsseriemodus.class, modus);

        agent.configure(registry.registry());
    }

    @Test
    public void skal_hente_tjenesteregisteret_fra_usercontexten() {
        final Map<String, Object> mock = spy(new HashMap<>());
        when(mock.get(ServiceRegistry.class.getSimpleName())).thenReturn(registry.registry());

        agent.configure(mock);

        verify(mock).get(ServiceRegistry.class.getSimpleName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void skal_ikkje_feile_sjoelv_om_partisjons_listener_feilar() {
        final PartisjonsListener listener = mock(PartisjonsListener.class);
        registry.registrer(PartisjonsListener.class, listener);

        final RuntimeException expected = new RuntimeException("You no take candle");
        doThrow(expected).when(listener).partitionInitialized(anyInt());

        agent.notifyListeners(context);

        verify(context, times(3)).emit(anyString(), eq(1));
    }
}