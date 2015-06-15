package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class WritablePathValidator implements IValueValidator<Path> {
    @Override
    public void validate(final String name, final Path value) throws ParameterException {
        if (!Files.exists(value)) {
            throw new ParameterException(
                    "Katalogen "
                            + value
                            + " eksisterer ikke, verifiser at du har angitt rett katalogsti."
            );
        }

        if (!Files.isDirectory(value)) {
            throw new ParameterException(
                    "Stien "
                            + value
                            + " peker ikke til en katalog, verifiser at du har angitt rett katalogsti."
            );
        }

        if (!Files.isReadable(value)) {
            throw new ParameterException(
                    "Innholdet i katalogen "
                            + value
                            + " er ikke lesbar for batchen, verifiser at batchbrukeren har lesetilgang til katalogen."
            );
        }

        if (!Files.isWritable(value)) {
            throw new ParameterException(
                    "Katalogen "
                            + value
                            + " er ikke skrivbar for batchen, verifiser at batchbrukeren har skrivetilgang til katalogen."
            );
        }
    }
}
