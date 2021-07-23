package no.spk.felles.tidsserie.batch.main.input;

import no.spk.faktura.input.DummyCommand;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/**
 * Validerer at et år ikke er tidligere nn 2007. Benyttes for å begrense tillatt årstall for bruk i sql-spørringer på historikk.
 * @see ProgramArguments
 */
public class YearValidator {

    public void validate(final String name, final Integer value) throws ParameterException {
        if (value < 2007) {
            throw new ParameterException(
                    new CommandLine(new DummyCommand()),
                    "'" + name + "': kan ikke være mindre enn 2007."
            );
        }
    }
}
