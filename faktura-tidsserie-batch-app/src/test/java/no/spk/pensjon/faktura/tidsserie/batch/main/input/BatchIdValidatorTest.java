package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import com.beust.jcommander.ParameterException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class BatchIdValidatorTest {

    private static final String SOME_PARAMETER = "param";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private BatchIdValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new BatchIdValidator();
    }

    @Test
     public void testInvalidFormat() throws Exception {
        exception.expect(ParameterException.class);
        exception.expectMessage("må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS");
        validator.validate(SOME_PARAMETER, "12");
    }

    @Test
    public void testValidFormat() throws Exception {
        validator.validate(SOME_PARAMETER, "grunnlagsdata_2015-01-01_01-01-01-01");
    }
}