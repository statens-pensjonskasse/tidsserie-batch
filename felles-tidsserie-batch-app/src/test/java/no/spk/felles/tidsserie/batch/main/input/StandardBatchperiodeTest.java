package no.spk.felles.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class StandardBatchperiodeTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void tiltenkt_bruk_for_tidspunkt_foer_2015_krever_tidsreiser_og_er_ikke_tatt_hoyde_for() throws Exception {
        exception.expect(IllegalArgumentException.class);
        new StandardBatchperiode(LocalDate.of(2015, 1, 1).minusDays(1));
    }

    @Test
    public void tilAar_skal_vaere_likt_argumentaaret() throws Exception {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2015, 1, 1));
        assertThat(periode.tilAar()).isEqualTo(2015);
    }

    @Test
    public void periodelengde_skal_vaere_maks_10_aar() throws Exception {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2020, 1, 1));
        assertThat(periode.fraAar()).isEqualTo(2011);
        assertThat(periode.tilAar() - periode.fraAar() + 1).isEqualTo(10);
    }

    @Test
    public void fraAar_skal_vaere_minimum_2007() throws Exception {
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.of(2015, 1, 1));
        assertThat(periode.fraAar()).isEqualTo(2007);
    }

    @Test
    public void program_arguments_benytter_stander_batchperiode_med_current_date() throws Exception {
        final ProgramArguments programArguments = new ProgramArguments();
        final StandardBatchperiode periode = new StandardBatchperiode(LocalDate.now());

        assertThat(programArguments.fraAar).isEqualTo(periode.fraAar());
        assertThat(programArguments.tilAar).isEqualTo(periode.tilAar());
    }
}