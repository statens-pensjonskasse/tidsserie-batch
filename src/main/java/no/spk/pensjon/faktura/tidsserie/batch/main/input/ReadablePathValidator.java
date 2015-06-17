package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class ReadablePathValidator implements IValueValidator<Path> {
    @Override
    public void validate(final String name, final Path value) throws ParameterException {
        if (!Files.exists(value)) {
            throw new ParameterException(
                    "Filen "
                            + value
                            + " eksisterer ikke, verifiser at du har angitt rett filnavn og -sti."
            );
        }

        if (!Files.isReadable(value)) {
            throw new ParameterException(
                    "Filen "
                            + value
                            + " er ikke lesbar for batchen, verifiser at batchbrukeren har lesetilgang til filen."
            );
        }
    }
}
