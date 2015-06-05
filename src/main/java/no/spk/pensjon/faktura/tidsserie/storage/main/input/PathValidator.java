package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class PathValidator implements IValueValidator<Optional<Path>> {
    @Override
    public void validate(final String name, final Optional<Path> value) throws ParameterException {
        if (!value.isPresent()) {
            return;
        }

        if (!Files.exists(value.get())) {
            throw new ParameterException(
                    "Filen "
                            + value.get()
                            + " eksisterer ikke, verifiser at du har angitt rett filnavn og -sti."
            );
        }

        if (!Files.isReadable(value.get())) {
            throw new ParameterException(
                    "Filen "
                            + value.get()
                            + " er ikke lesbar for batchen, verifiser at batchbrukeren har lesetilgang til filen."
            );
        }
    }
}
