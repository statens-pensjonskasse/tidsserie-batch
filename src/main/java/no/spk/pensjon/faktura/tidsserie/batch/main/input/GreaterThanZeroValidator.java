package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * Validerer at at et kommandolinjeargument kan transformeres til en {@link Integer} og at tallet er st�rre enn 0.
 * @author Snorre E. Brekke - Computas
 * @see ProgramArguments
 * @see com.beust.jcommander.JCommander
 */
public class GreaterThanZeroValidator implements IValueValidator<Number> {
    @Override
    public void validate(String name, Number value) throws ParameterException {
        if (value.intValue() <= 0) {
            throw new ParameterException("'" + name + "': m� v�re st�rre enn 0 (fant " + value +").");
        }
    }
}
