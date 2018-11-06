package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.erGyldig;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class AntallProsessorarValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String verdi) throws ParameterException {
        if (!verdi.matches("-?[0-9]+")) {
            throw new ParameterException("'" + name + "': er ikke et gyldig tall (fant " + verdi + ").");
        }

        if (!erGyldig(parseInt(verdi))) {
            throw new ParameterException(
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
