package no.spk.premie.tidsserie.batch.main;

import static no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.premie.tidsserie.batch.core.registry.Ranking.ranking;
import static no.spk.premie.tidsserie.batch.core.registry.Ranking.standardRanking;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import no.spk.faktura.input.BatchId;
import no.spk.premie.tidsserie.batch.core.BatchIdConstants;
import no.spk.premie.tidsserie.batch.core.Katalog;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.premie.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.premie.tidsserie.batch.core.TidsserieLivssyklusException;
import no.spk.premie.tidsserie.batch.core.Tidsseriemodus;
import no.spk.premie.tidsserie.batch.core.kommandolinje.Bruksveiledning;
import no.spk.premie.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.premie.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TidsserieBatchTest {

    @RegisterExtension
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @TempDir
    public Path temp;

    @Mock
    private Bruksveiledning bruksveiledning;

    @Mock(name = "a")
    private TidsserieLivssyklus a;

    @Mock
    private TidsserieLivssyklus b;

    @Mock
    private TidsserieLivssyklus c;

    @Mock
    private Tidsseriemodus modus;

    @Mock
    private ApplicationController controller;

    @Mock
    private TidsserieBatchArgumenter argumenter;

    private TidsserieBatch main;

    @BeforeEach
    void _before() {
        main = new TidsserieBatch(
                registry.registry(),
                exitCode -> {
                },
                controller
        );
        registry.registrer(TidsserieLivssyklus.class, a);
        registry.registrer(TidsserieLivssyklus.class, b);
        registry.registrer(TidsserieLivssyklus.class, c);

        registry.registrer(Path.class, temp, Katalog.UT.egenskap());
        registry.registrer(Path.class, temp, Katalog.LOG.egenskap());
        registry.registrer(Path.class, temp, Katalog.GRUNNLAGSDATA.egenskap());

        lenient().doReturn(antallProsessorar(1)).when(argumenter).antallProsessorar();
        registry.registrer(TidsserieBatchArgumenter.class, argumenter);
    }

    @Test
    void skal_ikkje_fange_error_frå_oppretting_av_kommandolinjeargument_parser() {
        final ManglandeServiceLoaderOppsettError expected = new ManglandeServiceLoaderOppsettError(TidsserieBatchArgumenterParser.class);

        assertThatCode(
                () -> main.run(parserSomKastarFølgjandeError(expected))
        )
                .as("batchen skal ikkje fange feil som er av type Error eller subtyper av den")
                .isInstanceOf(ManglandeServiceLoaderOppsettError.class)
                .isSameAs(expected);
    }

    @Test
    void skal_informere_brukaren_om_ugyldige_kommandolinjeargument() {
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
    void skal_vise_bruksveiledning_når_forespurt_av_brukaren() {
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
    void skal_fange_uventa_exceptions_frå_parsing_av_kommandolinjeargument_og_informere_brukaren_om_ukjent_feil() {
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
    void skal_registrere_argumenter_i_tjenesteregisteret() {
        main.run(
                () -> args -> argumenter
        );
        registry
                .assertTenesterAvType(TidsserieBatchArgumenter.class)
                .containsOnly(argumenter);
    }

    @Test
    void skal_kaste_exception_ved_feil_paa_ein_av_start_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).start(any());

        lagTidsserieOgIgnorerFeil(expected);
    }

    @Test
    void skal_ikkje_kaste_exception_ved_feil_paa_ein_av_stop_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).stop(any());

        lagTidsserieUtenAaForventeFeil();
    }

    @Test
    void skal_notifisere_controlleren_ved_feil_paa_ein_av_stop_livssyklusane() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(a).stop(any());

        lagTidsserieUtenAaForventeFeil();

        verify(controller).informerOmUkjentFeil(same(expected));
    }

    @Test
    void skal_kaste_exception_ved_feil_paa_controlleren_ved_lag_tidsserie() {
        final IllegalArgumentException expected = new IllegalArgumentException("this is the message");
        doThrow(expected).when(controller).lagTidsserie(any(), any());

        assertThatCode(this::lagTidsserie).isSameAs(expected);
    }

    @Test
    void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_start_feila() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(b).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    void skal_kalle_alle_generer_tidsserie_callback_selv_om_foerste_feilet() {
        final RuntimeException expected = new RuntimeException("callback error");

        final TidsserieGenerertCallback2 firstCallback = mock(TidsserieGenerertCallback2.class);
        willThrow(expected).given(firstCallback).tidsserieGenerert(any(), any());

        registry.registrer(TidsserieGenerertCallback2.class, firstCallback, ranking(1000).egenskap());

        TidsserieGenerertCallback2 secondCallback = mock(TidsserieGenerertCallback2.class);
        registry.registrer(TidsserieGenerertCallback2.class, secondCallback, standardRanking().egenskap());

        lagTidsserieOgIgnorerFeil(expected);

        verify(secondCallback).tidsserieGenerert(any(), any());
    }

    @SuppressWarnings("deprecation")
    @Test
    void skal_kalle_alle_gamle_generer_tidsserie_callback_selv_om_foerste_feilet() {
        final RuntimeException expected = new RuntimeException("callback error");

        final TidsserieGenerertCallback firstCallback = mock(TidsserieGenerertCallback.class);
        willThrow(expected).given(firstCallback).tidsserieGenerert(any());

        registry.registrer(TidsserieGenerertCallback.class, firstCallback, ranking(1000).egenskap());

        TidsserieGenerertCallback secondCallback = mock(TidsserieGenerertCallback.class);
        registry.registrer(TidsserieGenerertCallback.class, secondCallback, standardRanking().egenskap());

        lagTidsserieOgIgnorerFeil(expected);

        verify(secondCallback).tidsserieGenerert(any());
    }


    @Test
    void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_generer_tidsserie_callback_feila() {
        final RuntimeException expected = new RuntimeException("callback error");

        final TidsserieGenerertCallback2 callback = mock(TidsserieGenerertCallback2.class);
        willThrow(expected).given(callback).tidsserieGenerert(any(), any());

        registry.registrer(TidsserieGenerertCallback2.class, callback);

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @SuppressWarnings("deprecation")
    @Test
    void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_gamle_generer_tidsserie_callback_feila() {
        final RuntimeException expected = new RuntimeException("callback error");

        final TidsserieGenerertCallback callback = mock(TidsserieGenerertCallback.class);
        willThrow(expected).given(callback).tidsserieGenerert(any());

        registry.registrer(TidsserieGenerertCallback.class, callback);

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    void skal_ikkje_kalle_kontrolleren_dersom_start_feila() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(a).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(controller, never()).lagTidsserie(any(), any());
    }

    @Test
    void skal_kalle_start_paa_alle_livssyklusar_sjoelv_om_start_feila_paa_tidligare_tjeneste() {
        final RuntimeException expected = new RuntimeException("a says hello!");
        doThrow(expected).when(a).start(any());

        lagTidsserieOgIgnorerFeil(expected);

        verify(a).start(any());
        verify(b).start(any());
        verify(c).start(any());
    }

    @Test
    void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_stop_feila_paa_tidligare_tjeneste() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(b).stop(any());

        lagTidsserieUtenAaForventeFeil();

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    @Test
    void skal_kalle_stop_paa_alle_livssyklusar_sjoelv_om_controller_feilar() {
        final RuntimeException expected = new RuntimeException("b says hello!");
        doThrow(expected).when(controller).lagTidsserie(any(), any());

        assertThatCode(this::lagTidsserie).isSameAs(expected);

        verify(a).stop(any());
        verify(b).stop(any());
        verify(c).stop(any());
    }

    private void lagTidsserieOgIgnorerFeil(final Exception expected) {
        assertThatCode(this::lagTidsserie)
                .isInstanceOfAny(TidsserieLivssyklusException.class, TidsserieGenerertException.class)
                .hasSuppressedException(expected);
    }

    private void lagTidsserieUtenAaForventeFeil() {
        lagTidsserie();
    }

    private void lagTidsserie() {
        main.lagTidsserie(
                controller,
                modus,
                LocalDateTime.MIN,
                new BatchId(BatchIdConstants.TIDSSERIE_PREFIX, LocalDateTime.MIN)
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