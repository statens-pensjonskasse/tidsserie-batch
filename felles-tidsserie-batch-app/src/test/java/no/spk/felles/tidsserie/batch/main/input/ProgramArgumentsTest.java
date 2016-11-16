package no.spk.felles.tidsserie.batch.main.input;

import static no.spk.felles.tidsserie.batch.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;

import org.junit.Test;

public class ProgramArgumentsTest {
    @Test
    public void skal_generere_observasjonsperiode_fra_1_januar_til_31_desember_i_fra_og_til_aarstalla() {
        ProgramArguments args = new ProgramArguments();
        args.fraAar = 2010;
        args.tilAar = 2015;
        assertThat(args.observasjonsperiode())
                .isEqualTo(new Observasjonsperiode(dato("2010.01.01"), dato("2015.12.31")));
    }
}