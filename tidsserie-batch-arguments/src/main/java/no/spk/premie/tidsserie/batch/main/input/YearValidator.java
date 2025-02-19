package no.spk.premie.tidsserie.batch.main.input;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/**
 * Validerer at et år ikke er tidligere enn 2007. Benyttes for å begrense tillatt årstall for bruk i sql-spørringer på historikk.
 * @see ProgramArguments
 */
public class YearValidator {

    public void validate(final String name, final Integer value, final CommandSpec spec) throws ParameterException {
        if (value < 2007) {
            throw new ParameterException(
                    new CommandLine(spec),
                    "'" + name + "': kan ikke være mindre enn 2007."
            );
        }
    }
}
