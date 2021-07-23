package no.spk.felles.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine.ParameterException;

public class BatchIdValidatorTest {
    private static final String SOME_PARAMETER = "param";

    private BatchIdValidator validator;

    @Before
    public void setUp() {
        validator = new BatchIdValidator();
    }

    @Test
    public void testInvalidFormat() {
        assertThatCode(
                () -> validator.validate(SOME_PARAMETER, "12")
        )
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS");
    }

    @Test
    public void testValidFormat() {
        validator.validate(SOME_PARAMETER, "grunnlagsdata_2015-01-01_01-01-01-01");
    }
}
