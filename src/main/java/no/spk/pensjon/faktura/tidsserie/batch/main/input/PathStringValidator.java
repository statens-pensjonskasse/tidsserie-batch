package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class PathStringValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        try {
            Paths.get(value);
        } catch (InvalidPathException e) {
            throw new ParameterException(name + " er ikke en gyldig filbane: " + value, e);
        }
    }
}
