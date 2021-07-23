package no.spk.felles.tidsserie.batch.main.input;

import static java.util.stream.Collectors.joining;

import java.util.Optional;

import no.spk.faktura.input.DummyCommand;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/**
 * {@link ModusValidator} verifiserer
 * at den angitte moduskoda tilhøyrer ein av modusane som batchen støttar.
 *
 * @see Modus
 */
public class ModusValidator {
    public void validate(final String name, final String value) throws ParameterException {
        final Optional<Modus> modus = Modus.parse(value);
        if (!modus.isPresent()) {
            throwParameterException(value);
        }
    }

    private void throwParameterException(final String value) {
        throw new ParameterException(
                new CommandLine(new DummyCommand()),
                feilmelding(value)
        );
    }

    static String feilmelding(final String value) {
        return "Modus '" + value + "' er ikkje støtta av felles-tidsserie-batch.\n"
                + "\nFølgjande modusar er støtta:\n"
                + Modus.stream()
                .map(Modus::kode)
                .map(k -> "- " + k)
                .collect(joining("\n"));
    }
}
