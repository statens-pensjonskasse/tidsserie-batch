package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class IntegerValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        try {
            int i = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParameterException("'" + name + "': er ikke et gyldig tall (fant " + value + ").");
        }
    }
}
