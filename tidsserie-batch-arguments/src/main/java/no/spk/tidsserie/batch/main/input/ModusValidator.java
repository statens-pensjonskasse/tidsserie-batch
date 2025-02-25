package no.spk.tidsserie.batch.main.input;

import static java.util.stream.Collectors.joining;

import java.util.Optional;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/**
 * {@link ModusValidator} verifiserer
 * at den angitte moduskoda tilhøyrer ein av modusane som batchen støttar.
 *
 * @see Modus
 */
public class ModusValidator {

    public void validate(final String name, final String value, final CommandSpec spec) throws ParameterException {
        final Optional<Modus> modus = Modus.parse(value);
        if (!modus.isPresent()) {
            throwParameterException(value, spec);
        }
    }

    private void throwParameterException(final String value, final CommandSpec spec) {
        throw new ParameterException(
                new CommandLine(spec),
                feilmelding(value)
        );
    }

    static String feilmelding(final String value) {
        return "Modus '" + value + "' er ikkje støtta av tidsserie-batch.\n"
                + "\nFølgjande modusar er støtta:\n"
                + Modus.stream()
                .map(Modus::kode)
                .map(k -> "- " + k)
                .collect(joining("\n"));
    }
}
