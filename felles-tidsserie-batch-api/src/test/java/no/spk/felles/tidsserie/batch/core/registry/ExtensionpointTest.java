package no.spk.felles.tidsserie.batch.core.registry;

import static no.spk.felles.tidsserie.batch.core.registry.Ranking.ranking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import java.util.stream.Collectors;

import no.spk.felles.tidsserie.batch.core.ServiceRegistryRule;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class ExtensionpointTest {
    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock(name = "a")
    private TidsserieLivssyklus a;

    @Mock(name = "b")
    private TidsserieLivssyklus b;

    @Mock(name = "c")
    private TidsserieLivssyklus c;

    private Extensionpoint<TidsserieLivssyklus> extensionpoint;

    @Test
    public void skal_kun_kalle_hoegast_rangerte_tjeneste() {
        final TidsserieLivssyklus d = mock(TidsserieLivssyklus.class, "d");
        registry.registrer(TidsserieLivssyklus.class, d, ranking(1000).egenskap());

        extensionpoint.invokeFirst(l -> l.start(registry.registry()));

        verify(d, times(1)).start(any());
        verify(a, never()).start(any());
        verify(b, never()).start(any());
        verify(c, never()).start(any());
    }

    @Test
    public void skal_kalle_alle_extensions_sjoelv_om_ein_av_dei_feilar() {
        // Setter her opp rekkefølga a (feilar), b (ok), c(ok) for å illustrere
        // ein situasjon der vi ønskjer at både b og c skal få callbacks sjølv
        // ein høgare rangert/tidligare extension feilar
        doThrow(new RuntimeException()).when(a).start(any());

        final Extensionpoint<TidsserieLivssyklus> extensionpoint = new Extensionpoint<>(TidsserieLivssyklus.class, registry.registry());
        extensionpoint.invokeAll(l -> l.start(registry.registry()));

        verify(a).start(any());
        verify(b).start(any());
        verify(c).start(any());
    }

    @Test
    public void skal_fange_feil_og_indikere_at_ein_extension_har_feila_via_status() {
        doThrow(new RuntimeException("skal_fange_feil_og_indikere_at_ein_extension_har_feila_via_status")).when(a).start(any());

        final ExtensionpointStatus status = invokeAll();
        assertHasFailed(status).isTrue();
        assertThat(status.stream().toList())
                .as("feil fra status " + status)
                .hasSize(1);
    }

    @Test
    public void skal_ikkje_kaste_exception_dersom_status_er_ok() {
        final ExtensionpointStatus status = invokeAll();
        status.orElseThrow(errors -> new AssertionError("Skulle ikkje blitt kasta"));
    }

    @Test
    public void skal_kaste_exception_dersom_status_ikkje_er_ok() {
        doThrow(new RuntimeException("I don't care")).when(a).start(any());

        assertThatCode(
                () ->
                        invokeAll()
                                .orElseThrow(
                                        errors -> new IllegalArgumentException("Antall feil er lik " + errors.count())
                                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Antall feil er lik 1");
    }

    @Test
    public void skal_merge_to_ok_som_ok() {
        final ExtensionpointStatus status = ExtensionpointStatus.merge(
                ExtensionpointStatus.ok(),
                ExtensionpointStatus.ok()
        );
        assertHasFailed(status).isFalse();
    }

    @Test
    public void skal_merge_feil_fra_begge_sjoelv_om_kun_ein_feilar() {
        final RuntimeException expected = new RuntimeException("first");
        doThrow(expected).when(a).start(any());
        final ExtensionpointStatus x = invokeAll();

        doNothing().when(a).start(any());
        assertHasFailed(x).isTrue();

        final ExtensionpointStatus y = invokeAll();
        assertHasFailed(y).isFalse();

        final ExtensionpointStatus status = ExtensionpointStatus.merge(x, y);
        assertHasFailed(status).isTrue();

        assertThat(status.stream().collect(Collectors.toList()))
                .hasSize(1)
                .containsOnly(expected);
    }

    @Test
    public void skal_merge_feil_fra_begge_statusane_om_dei_feilar() {
        final RuntimeException first = new RuntimeException("first");
        doThrow(first).when(a).start(any());
        final ExtensionpointStatus x = invokeAll();

        final RuntimeException second = new RuntimeException("second");
        doThrow(second).when(a).start(any());
        final ExtensionpointStatus y = invokeAll();

        final ExtensionpointStatus status = ExtensionpointStatus.merge(x, y);
        assertHasFailed(status).isTrue();

        assertThat(status.stream().collect(Collectors.toList())).containsOnly(first, second);
    }

    @Test
    public void skal_ikkje_fange_non_runtime_exceptions() {
        class SneakyThrowingLivssyklus implements TidsserieLivssyklus {

            private final Exception expected;

            private SneakyThrowingLivssyklus(final Exception expected) {
                this.expected = expected;
            }

            @Override
            public void start(final ServiceRegistry registry) {
                sneakyThrow(expected);
            }

            @SuppressWarnings("unchecked")
            private <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
                throw (T) t;
            }
        }
        final Exception expected = new Exception("SKYTE MEG SJØLV I FOTEN? JA TAKK!");
        registry.registrer(TidsserieLivssyklus.class, new SneakyThrowingLivssyklus(expected));
        try {
            invokeAll();
            fail("Her skulle Extensionpoint ha feila brutalt umiddelbart uten å fange feilen");
        } catch (final Exception e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Test
    public void skal_ikkje_fange_errors() {
        final Error expected = new Error("OH GOD, MY LEG, MY LEG!");
        doThrow(expected).when(a).start(any());

        try {
            invokeAll();
            fail("Her skulle Extensionpoint ha feila brutalt umiddelbart uten å fange feilen");
        } catch (final Error e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Test
    public void skal_rethrowe_foerste_feil_as_is() {
        final RuntimeException expected = new RuntimeException("DEN FYRSTE FEIL EG HØYRA FEKK VAR RUNTIMEEXCEPTION I VOGGA");
        doThrow(expected).when(a).start(any());

        try {
            invokeAll().orElseRethrowFirstFailure();
            fail("Her skulle den første feilen ha blitt rekasta");
        } catch (final RuntimeException e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Before
    public void _before() {
        extensionpoint = new Extensionpoint<>(TidsserieLivssyklus.class, registry.registry());

        registry.registrer(TidsserieLivssyklus.class, a);
        registry.registrer(TidsserieLivssyklus.class, b);
        registry.registrer(TidsserieLivssyklus.class, c);
    }

    private static AbstractBooleanAssert<?> assertHasFailed(final ExtensionpointStatus status) {
        return assertThat(status.hasFailed())
                .as("feila nokon av tjenestene som vart kalla?\nStatus: " + status);
    }

    private ExtensionpointStatus invokeAll() {
        return extensionpoint.invokeAll(l -> l.start(registry.registry()));
    }
}
