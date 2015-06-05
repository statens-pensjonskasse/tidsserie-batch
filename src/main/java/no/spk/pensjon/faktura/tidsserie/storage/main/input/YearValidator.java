package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * Validerer at et år ikke er tidligere nn 2007. Benyttes for å begrense tillatt årstall for bruk i sql-spørringer på historikk.
 * @author Snorre E. Brekke - Computas
 * @see com.beust.jcommander.JCommander
 * @see ProgramArguments
 */
public class YearValidator implements IValueValidator<Integer> {
    @Override
    public void validate(String name, Integer value) throws ParameterException {
        if (value < 2007) {
            throw new ParameterException("'" + name + "': kan ikke være mindre enn 2007.");
        }
    }
}
