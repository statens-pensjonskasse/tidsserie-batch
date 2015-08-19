package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static org.junit.Assume.assumeTrue;

import com.beust.jcommander.ParameterException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class PathStringValidatorTest {

    private static final String SOME_PARAM = "-param";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    PathStringValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new PathStringValidator();
    }

    @Test
    public void testIncorrectPathThrowsException() throws Exception {
        assumeTrue( isWindows() );
        exception.expect(ParameterException.class);
        exception.expectMessage(SOME_PARAM + " er ikke en gyldig filbane");
        validator.validate(SOME_PARAM, "H\"");
    }

    @Test
    public void testCorrectPathValidatesWithoutException() throws Exception {
        validator.validate(SOME_PARAM, "H:");
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}