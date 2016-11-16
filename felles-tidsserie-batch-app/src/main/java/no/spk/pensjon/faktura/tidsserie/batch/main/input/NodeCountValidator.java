package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * Validerer at at et kommandolinjeargument kan transformeres til en {@link Integer} og at tallet er større enn 0.
 * @author Snorre E. Brekke - Computas
 * @see ProgramArguments
 * @see com.beust.jcommander.JCommander
 */
public class NodeCountValidator implements IValueValidator<Number> {
    @Override
    public void validate(String name, Number value) throws ParameterException {
        int cpus = Runtime.getRuntime().availableProcessors();
        if (value.intValue() <= 0 || value.intValue() > cpus) {
            throw new ParameterException("'" + name + "': må være større enn 0 og kan ikke være større enn antall " +
                    "CPU'er på serveren (" + cpus  + ") - (fant " + value +").");
        }
    }
}
