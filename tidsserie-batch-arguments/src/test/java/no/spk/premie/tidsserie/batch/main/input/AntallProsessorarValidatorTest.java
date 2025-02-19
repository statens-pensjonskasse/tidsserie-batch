package no.spk.premie.tidsserie.batch.main.input;

import static java.lang.String.format;
import static java.util.stream.IntStream.rangeClosed;

import java.util.stream.Stream;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@ExtendWith(SoftAssertionsExtension.class)
public class AntallProsessorarValidatorTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    private final AntallProsessorarValidator validator = new AntallProsessorarValidator();

    @Test
    void skal_godta_tall_mellom_1_og_antall_tilgjengelige_prosessorar() {
        rangeClosed(1, tilgjengeligeProsessorar())
                .boxed()
                .forEach(
                        antall -> assertValideringsfeil(antall.toString()).doesNotThrowAnyException()
                );
    }

    @Test
    void skal_feile_når_verdi_inneholder_et_desimaltall() {
        assertValideringsfeil("1.2")
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("'n': er ikke et gyldig tall (fant 1.2)")
        ;
    }

    @Test
    void skal_feile_når_verdi_ikke_inneholder_en_integer() {
        assertValideringsfeil("t")
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("'n': er ikke et gyldig tall (fant t)")
        ;
    }

    @Test
    void skal_feile_når_verdi_ikke_er_gyldig_som_antall_prosessorer() {
        Stream.of("-1", "0")
                .forEach(
                        verdi -> assertValideringsfeil(verdi)
                                .isInstanceOf(ParameterException.class)
                                .hasMessageContaining("må være større enn 0")
                                .hasMessageContaining(verdi)
                );
    }

    @Test
    void skal_feile_når_verdi_er_større_enn_antall_tilgjengelige_prosessorar_på_maskina() {
        int antallProsessorar = tilgjengeligeProsessorar();
        assertValideringsfeil(
                "" + (antallProsessorar + 1)
        )
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("må være større enn 0 og kan ikke være større enn antall CPU'er på serveren")
                .hasMessageContaining(
                        format(
                                "(antall prosessorar %d) - (fant %d)",
                                antallProsessorar,
                                antallProsessorar + 1
                        )
                )
        ;
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertValideringsfeil(final String verdi) {
        return softly.assertThatCode(
                () -> validator.validate("n", verdi, CommandSpec.create())
        )
                .as(
                        "%s.validate(\"n\", \"%s\"",
                        validator.getClass().getSimpleName(),
                        verdi
                );
    }

    private int tilgjengeligeProsessorar() {
        return Runtime.getRuntime().availableProcessors();
    }
}