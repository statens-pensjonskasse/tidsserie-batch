package no.spk.felles.tidsserie.batch.main.input;

import java.util.stream.Stream;

import com.beust.jcommander.ParameterException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ModusValidatorTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Rule
    public final ModusRule modusar = new ModusRule();

    @After
    public void _after() {
        Modus.reload(Stream.empty());
    }

    @Test
    public void skalGodtaAlleKjenteModusarSineKoder() {
        final String navn = "navn på modus";
        modusar.support(navn);

        new ModusValidator().validate("modus", navn);
    }

    @Test
    public void skalAvviseUkjenteKoder() {
        e.expect(ParameterException.class);
        e.expectMessage("Modus 'whatever' er ikkje støtta av faktura-tidsserie-batch.");
        e.expectMessage("Følgjande modusar er støtta:");

        Modus.stream().map(Modus::kode).forEach(e::expectMessage);

        new ModusValidator().validate("modus", "whatever");
    }
}