package no.spk.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
class StandardBatchperiodeTest {
    @Test
    void tiltenkt_bruk_for_tidspunkt_foer_2015_krever_tidsreiser_og_er_ikke_tatt_hoyde_for() {
        assertThatCode(
                () -> new StandardBatchperiode(
                        LocalDate.of(2015, 1, 1).minusDays(1)
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tilAar_skal_vaere_likt_argumentaaret() {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2015, 1, 1));
        assertThat(periode.tilAar()).isEqualTo(2015);
    }

    @Test
    void periodelengde_skal_vaere_maks_10_aar() {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2020, 1, 1));
        assertThat(periode.fraAar()).isEqualTo(2011);
        assertThat(periode.tilAar() - periode.fraAar() + 1).isEqualTo(10);
    }

    @Test
    void fraAar_skal_vaere_minimum_2007() {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2015, 1, 1));
        assertThat(periode.fraAar()).isEqualTo(2007);
    }

    @Test
    void program_arguments_benytter_stander_batchperiode_med_current_date() {
        final ProgramArguments programArguments = new ProgramArguments();
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.now());

        assertThat(programArguments.fraAar).isEqualTo(periode.fraAar());
        assertThat(programArguments.tilAar).isEqualTo(periode.tilAar());
    }
}