package no.spk.premie.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

class BatchIdValidatorTest {
    private static final String SOME_PARAMETER = "param";

    private BatchIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BatchIdValidator();
    }

    @Test
    void invalidFormat() {
        assertThatCode(
                () -> validator.validate(SOME_PARAMETER, "12", CommandSpec.create())
        )
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS");
    }

    @Test
    void validFormat() {
        validator.validate(SOME_PARAMETER, "grunnlagsdata_2015-01-01_01-01-01-01", CommandSpec.create());
    }
}
