package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static java.util.stream.Collectors.joining;

import java.util.Optional;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * {@link no.spk.pensjon.faktura.tidsserie.batch.main.input.ModusValidator} verifiserer
 * at den angitte moduskoda tilhøyrer ein av modusane som batchen støttar.
 *
 * @see no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus
 */
public class ModusValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        final Optional<Modus> modus = Modus.parse(value);
        if (!modus.isPresent()) {
            throwParameterException(value);
        }
    }

    private void throwParameterException(final String value) {
        throw new ParameterException(
                feilmelding(value)
        );
    }

    static String feilmelding(final String value) {
        return "Modus '" + value + "' er ikkje støtta av faktura-tidsserie-batch.\n"
                + "\nFølgjande modusar er støtta:\n"
                + Modus.stream()
                .map(Modus::kode)
                .map(k -> "- " + k)
                .collect(joining("\n"));
    }
}
