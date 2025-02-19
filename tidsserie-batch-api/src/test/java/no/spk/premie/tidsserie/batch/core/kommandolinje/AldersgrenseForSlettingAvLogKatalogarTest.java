package no.spk.premie.tidsserie.batch.core.kommandolinje;

import static java.util.stream.IntStream.range;
import static no.spk.premie.tidsserie.batch.core.Datoar.dato;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.FinnLogkatalogerOperasjon;

import org.assertj.core.api.AbstractLocalDateTimeAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
public class AldersgrenseForSlettingAvLogKatalogarTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Mock
    private FinnLogkatalogerOperasjon søkeoperasjon;

    @Test
    void skal_godta_aldersgrenser_på_0_dagar_og_større() {
        range(0, 366)
                .boxed()
                .map(this::assertNewAldersgrense)
                .forEach(AbstractThrowableAssert::doesNotThrowAnyException);
    }

    @Test
    void skal_ikkje_godta_negative_aldersgrenser() {
        Stream.of(-1, -5, -7, -14, -365, -366)
                .map(this::assertNewAldersgrense)
                .forEach(
                        assertion -> assertion.isInstanceOf(IllegalArgumentException.class)
                );
    }

    @Test
    void skal_eksekvere_søkeoperasjon_dersom_aldersgrense_er_over_0_dagar() throws IOException {
        final Path expected = Paths.get("yadayada");
        when(søkeoperasjon.finn(any())).thenReturn(Stream.of(expected));

        assertFinnSlettbareLogkatalogar(1).containsOnly(expected);

        verify(søkeoperasjon).finn(any());
    }

    @Test
    void skal_ikkje_eksekvere_søkeoperasjon_dersom_aldersgrense_er_lik_0_dagar() throws IOException {
        assertFinnSlettbareLogkatalogar(0).isEmpty();

        verify(søkeoperasjon, never()).finn(any());
    }

    @Test
    void skal_regne_ut_klokkeslettet_som_regulerer_at_logkatalogar_eldre_enn_klokkeslettet_vil_bli_sletta() {
        assertCutoff(5, "2017.08.15").isEqualTo("2017-08-11T00:00:00");

        assertCutoff(0, "2016.02.29").isEqualTo("2016-03-01T00:00:00");
        assertCutoff(1, "2016.02.29").isEqualTo("2016-02-29T00:00:00");
        assertCutoff(2, "2016.02.29").isEqualTo("2016-02-28T00:00:00");

        assertCutoff(5, "2013.01.01").isEqualTo("2012-12-28T00:00:00");
    }

    @Test
    void skal_regne_ut_klokkeslett_relativt_til_dagens_dato() throws IOException {
        final int antallDagar = 2173;
        final LocalDateTime expected = LocalDate.now().atStartOfDay().plusDays(1).minusDays(antallDagar);

        aldersgrenseForSlettingAvLogKatalogar(antallDagar)
                .finnSlettbareLogkatalogar(søkeoperasjon);

        verify(søkeoperasjon).finn(eq(expected));
    }

    private AbstractLocalDateTimeAssert<?> assertCutoff(final int antallDagar, final String dagensDato) {
        return assertThat(
                aldersgrenseForSlettingAvLogKatalogar(antallDagar).cutoff(dato(dagensDato))
        )
                .as(
                        "aldersgrense(%d).cutoff(%s)",
                        antallDagar,
                        dagensDato
                );
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertNewAldersgrense(final Integer antallDagar) {
        return softly.assertThatCode(
                () -> aldersgrenseForSlettingAvLogKatalogar(antallDagar)
        )
                .as(
                        "new %s(%d)",
                        AldersgrenseForSlettingAvLogKatalogar.class.getSimpleName(),
                        antallDagar
                );
    }

    private ListAssert<Path> assertFinnSlettbareLogkatalogar(final int antallDagar) throws IOException {
        return assertThat(
                aldersgrenseForSlettingAvLogKatalogar(antallDagar)
                        .finnSlettbareLogkatalogar(søkeoperasjon)
        )
                .as("aldersgrense(%d).finnSlettbareLogkatalogar(<søkeoperasjon>)");
    }
}