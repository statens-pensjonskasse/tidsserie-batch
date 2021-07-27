package no.spk.felles.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.stream.Stream;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class ModusValidatorTest {
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

        new ModusValidator().validate("modus", navn, CommandSpec.create());
    }

    @Test
    public void skalAvviseUkjenteKoder() {
        final AbstractThrowableAssert<?, ?> assertion = assertThatCode(
                () -> new ModusValidator().validate("modus", "whatever", CommandSpec.create())
        )
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Modus 'whatever' er ikkje støtta av felles-tidsserie-batch.")
                .hasMessageContaining("Følgjande modusar er støtta:");

        Modus
                .stream()
                .map(Modus::kode)
                .forEach(assertion::hasMessageContaining);
    }
}