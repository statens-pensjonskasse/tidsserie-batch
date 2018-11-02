package no.spk.felles.tidsserie.batch.main;

import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.felles.tidsserie.batch.core.registry.Ranking.ranking;
import static no.spk.felles.tidsserie.batch.core.registry.Ranking.standardRanking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import java.nio.file.Path;
import java.util.function.Supplier;

import no.spk.felles.tidsserie.batch.ServiceRegistryRule;
import no.spk.felles.tidsserie.batch.TemporaryFolderWithDeleteVerification;
import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklusException;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.Bruksveiledning;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class TidsserieBatchTest {
    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    @Mock
    private Bruksveiledning bruksveiledning;

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

    @Mock
    private TidsserieBatchArgumenter argumenter;

    private TidsserieBatch main;

    @Before
    public void _before() {
        main = new TidsserieBatch(
                registry.registry(),
                exitCode -> {},
                controller
        );
        registry.registrer(TidsserieLivssyklus.class, a);
        registry.registrer(TidsserieLivssyklus.class, b);
        registry.registrer(TidsserieLivssyklus.class, c);

        registry.registrer(Path.class, temp.getRoot().toPath(), Katalog.UT.egenskap());

        lenient().doReturn(antallProsessorar(1)).when(argumenter).antallProsessorar();
        registry.registrer(TidsserieBatchArgumenter.class, argumenter);
    }

    @Test
    public void skal_ikkje_fange_error_frå_oppretting_av_kommandolinjeargument_parser() {
        final ManglandeServiceLoaderOppsettError expected = new ManglandeServiceLoaderOppsettError(TidsserieBatchArgumenterParser.class);

        assertThatCode(
                () -> main.run(parserSomKastarFølgjandeError(expected))
        )
                .as("batchen skal ikkje fange feil som er av type Error eller subtyper av den")
                .isInstanceOf(ManglandeServiceLoaderOppsettError.class)
                .isSameAs(expected);
    }

    @Test
    public void skal_informere_brukaren_om_ugyldige_kommandolinjeargument() {
        final UgyldigKommandolinjeArgumentException expected = new UgyldigKommandolinjeArgumentException(
                "Yada yada",
                bruksveiledning
        );

        assertThatCode(
                () -> main.run(parserSomKastarFølgjandeException(expected))
        )
                .as("Batchen skal handtere ugyldige kommandolinjeargument uten å feile")
                .doesNotThrowAnyException();

        verify(controller /*    */).informerOmUgyldigeArgumenter(expected);

        verify(controller, never()).informerOmBruk(any());
        verify(controller, never()).informerOmUkjentFeil(any());
    }

    @Test
    public void skal_vise_bruksveiledning_når_forespurt_av_brukaren() {
        final BruksveiledningSkalVisesException expected = new BruksveiledningSkalVisesException(bruksveiledning);

        assertThatCode(
                () -> main.run(parserSomKastarFølgjandeException(expected))
        )
                .as("Batchen skal takle visning av bruksveiledning uten å feile")
                .doesNotThrowAnyException();

        verify(controller /*    */).informerOmBruk(expected);

        verify(controller, never()).informerOmUgyldigeArgumenter(any());
        verify(controller, never()).informerOmUkjentFeil(any());
    }

    @Test
    public void skal_fange_uventa_exceptions_frå_parsing_av_kommandolinjeargument_og_informere_brukaren_om_ukjent_feil() {
        final RuntimeException e = new RuntimeException("Oops, I didn't mean to do it again");
        final TidsserieBatchArgumenterParser parser = args -> {
            throw e;
        };

        assertThatCode(
                () -> main.run(() -> parser)
        )
                .as(
                        "batchen skal fange alle exceptions som oppstår i parsing av kommandolinjeargument"
                )
                .doesNotThrowAnyException();
        verify(controller).informerOmUkjentFeil(e);

        verify(controller, never()).informerOmUgyldigeArgumenter(any());
    }

    @Test
    public void skal_registrere_argumenter_i_tjenesteregisteret() {
        main.run(
                () -> args -> argumenter
        );
        registry
                .assertTenesterAvType(TidsserieBatchArgumenter.class)
                .containsOnly(argumenter);
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
        doThrow(expected).when(controller).lagTidsserie(any(), any());

        try {
            main.lagTidsserie(
                    controller,
                    modus
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
        registry.registrer(TidsserieGenerertCallback.class, firstCallback, ranking(1000).egenskap());

        TidsserieGenerertCallback secondCallback = mock(TidsserieGenerertCallback.class);
        registry.registrer(TidsserieGenerertCallback.class, secondCallback, standardRanking().egenskap());

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

        verify(controller, never()).lagTidsserie(any(), any());
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
        doThrow(expected).when(controller).lagTidsserie(any(), any());

        try {
            main.lagTidsserie(
                    controller,
                    modus
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
                    modus
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
                modus
        );
    }

    private Supplier<TidsserieBatchArgumenterParser> parserSomKastarFølgjandeException(final RuntimeException e) {
        return () -> {
            throw e;
        };
    }

    private Supplier<TidsserieBatchArgumenterParser> parserSomKastarFølgjandeError(final Error e) {
        return () -> {
            throw e;
        };
    }
}