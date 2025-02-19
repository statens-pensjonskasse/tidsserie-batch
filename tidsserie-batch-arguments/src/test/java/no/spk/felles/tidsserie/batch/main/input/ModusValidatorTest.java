package no.spk.felles.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.stream.Stream;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class ModusValidatorTest {

    @RegisterExtension
    public final ModusExtension modusar = new ModusExtension();

    @AfterEach
    void _after() {
        Modus.reload(Stream.empty());
    }

    @Test
    void skalGodtaAlleKjenteModusarSineKoder() {
        final String navn = "navn på modus";
        modusar.support(navn);

        new ModusValidator().validate("modus", navn, CommandSpec.create());
    }

    @Test
    void skalAvviseUkjenteKoder() {
        final AbstractThrowableAssert<?, ?> assertion = assertThatCode(
                () -> new ModusValidator().validate("modus", "whatever", CommandSpec.create())
        )
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Modus 'whatever' er ikkje støtta av tidsserie-batch.")
                .hasMessageContaining("Følgjande modusar er støtta:");

        Modus
                .stream()
                .map(Modus::kode)
                .forEach(assertion::hasMessageContaining);
    }
}