package no.spk.felles.tidsserie.batch.main;

import static no.spk.pensjon.faktura.tjenesteregister.Constants.SERVICE_RANKING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.ServiceRegistryRule;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertException;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklusException;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TidsserieMainTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock(name = "a")
    private TidsserieLivssyklus a;

    @Mock(name = "b")
    private TidsserieLivssyklus b;

    @Mock(name = "c")
    private TidsserieLivssyklus c;

    @Mock(name = "modus")
    private Tidsseriemodus modus;

    @Mock(name = "controller")
    private ApplicationController controller;

    private final Observasjonsperiode periode = new Observasjonsperiode(
            new Aarstall(2015).atStartOfYear(),
            new Aarstall(2015).atEndOfYear()
    );

    private TidsserieMain main;

    private Integer exitCode;

    @Before
    public void _before() {
        main = new TidsserieMain(
                registry.registry(),
                exitCode -> this.exitCode = exitCode,
                controller
        );
        registry.registrer(TidsserieLivssyklus.class, a);
        registry.registrer(TidsserieLivssyklus.class, b);
        registry.registrer(TidsserieLivssyklus.class, c);
    }

    @Test
    public void skal_kaste_exception_ved_feil_paa_ein_av_start_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).start(any());

        lagTidsserieMenForventFeil(expected);
    }

    @Test
    public void skal_ikkje_kaste_exception_ved_feil_paa_ein_av_stop_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).stop(any());

        lagTidsserieUtenAaForventeFeil();
    }

    @Test
    public void skal_notifisere_controlleren_ved_feil_paa_ein_av_stop_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).stop(any());

        lagTidsserieUtenAaForventeFeil();

        verify(controller).informerOmUkjentFeil(same(expected));
    }

    @Test
    public void skal_kaste_exception_ved_feil_paa_controlleren_ved_lag_tidsserie() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(controller).lagTidsserie(any(), any(), any());

        try {
            main.lagTidsserie(
                    controller,
                    modus,
                    periode
            );
            fail("Skulle ha feila ettersom ein livssyklusane eller controlleren feila");
        } catch (final RuntimeException e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Test
    public void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_start_feila() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(b).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    public void skal_kalle_alle_generer_tidsserie_callback_selv_om_foerste_feilet() {
        final RuntimeException expected = new RuntimeException("callback error");
        final TidsserieGenerertCallback firstCallback = r -> {
            throw expected;
        };
        registry.registrer(TidsserieGenerertCallback.class, firstCallback, SERVICE_RANKING + "=1000");

        TidsserieGenerertCallback secondCallback = mock(TidsserieGenerertCallback.class);
        registry.registrer(TidsserieGenerertCallback.class, secondCallback, SERVICE_RANKING + "=0");

        lagTidsserieOgIgnorerFeil(expected);

        verify(secondCallback).tidsserieGenerert(any());
    }


    @Test
    public void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_generer_tidsserie_callback_feila() {
        final RuntimeException expected = new RuntimeException("callback error");
        registry.registrer(TidsserieGenerertCallback.class, r -> {
            throw expected;
        });

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    public void skal_ikkje_kalle_kontrolleren_dersom_start_feila() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(a).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(controller, never()).lagTidsserie(any(), any(), any());
    }

    @Test
    public void skal_kalle_start_paa_alle_livssyklusar_sjoelv_om_start_feila_paa_tidligare_tjeneste() {
        final RuntimeException expected = new RuntimeException("a says hello!");
        doThrow(expected).when(a).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).start(any());
        verify(b).start(any());
        verify(c).start(any());
    }

    @Test
    public void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_stop_feila_paa_tidligare_tjeneste() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(b).stop(any());

        lagTidsserieUtenAaForventeFeil();

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    public void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_controller_feilar() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(controller).lagTidsserie(any(), any(), any());

        try {
            main.lagTidsserie(
                    controller,
                    modus,
                    periode
            );
            fail("Skulle ha feila ettersom ein livssyklusane eller controlleren feila");
        } catch (final RuntimeException e) {
            assertThat(e).isSameAs(expected);
        }

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    private void lagTidsserieOgIgnorerFeil(final Exception expected) {
        try {
            main.lagTidsserie(
                    controller,
                    modus,
                    periode
            );
            fail("Skulle ha feila ettersom ein livssyklusane eller controlleren feila");
        } catch (final TidsserieLivssyklusException | TidsserieGenerertException e) {
            assertThat(e.getSuppressed()).contains(expected);
        }
    }

    private void lagTidsserieMenForventFeil(final Exception expected) {
        lagTidsserieOgIgnorerFeil(expected);
    }

    private void lagTidsserieUtenAaForventeFeil() {
        main.lagTidsserie(
                controller,
                modus,
                periode
        );
    }
}