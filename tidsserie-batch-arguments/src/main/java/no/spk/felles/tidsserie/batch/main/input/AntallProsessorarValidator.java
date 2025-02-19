package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.erGyldig;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class AntallProsessorarValidator {

    public void validate(final String name, final String verdi, final CommandSpec spec) throws ParameterException {
        if (!verdi.matches("-?[0-9]+")) {
            throw new ParameterException(
                    new CommandLine(spec),
                    "'" + name + "': er ikke et gyldig tall (fant " + verdi + ")."
            );
        }

        if (!erGyldig(parseInt(verdi))) {
            throw new ParameterException(
                    new CommandLine(spec),
                    format(
                            "'%s': må være større enn 0 og kan ikke være større enn antall CPU'er på serveren (%s) - (fant %s).",
                            name,
                            availableProcessors(),
                            verdi
                    )
            );
        }
    }
}
