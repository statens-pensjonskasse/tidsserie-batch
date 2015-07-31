package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static java.util.Arrays.asList;

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ModusValidatorTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void skalGodtaAlleKjenteModusarSineKoder() {
        asList(Modus.values()).forEach(m -> {
            new ModusValidator().validate("modus", m.kode());
        });
    }

    @Test
    public void skalAvviseUkjenteKoder() {
        e.expect(ParameterException.class);
        e.expectMessage("Modus 'whatever' er ikkje støtta av faktura-tidsserie-batch.");
        e.expectMessage("Følgjande modusar er støtta:");
        for (final Modus modus : Modus.values()) {
            e.expectMessage(modus.kode());
        }

        new ModusValidator().validate("modus", "whatever");
    }
}