package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * Validerer at et �r ikke er tidligere nn 2007. Benyttes for � begrense tillatt �rstall for bruk i sql-sp�rringer p� historikk.
 * @author Snorre E. Brekke - Computas
 * @see com.beust.jcommander.JCommander
 * @see ProgramArguments
 */
public class YearValidator implements IValueValidator<Integer> {
    @Override
    public void validate(String name, Integer value) throws ParameterException {
        if (value < 2007) {
            throw new ParameterException("'" + name + "': kan ikke v�re mindre enn 2007.");
        }
    }
}
