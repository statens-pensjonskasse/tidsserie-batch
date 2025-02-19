package no.spk.premie.tidsserie.batch.it;


import static java.lang.String.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Stream;

import no.spk.premie.tidsserie.batch.plugins.grunnlagsdatavalidator.Md5sum;

class Md5Checksums {
    private final Md5sum md5sum = new Md5sum();

    void generer(final File grunnlagsdata) {
        final File checksums = new File(grunnlagsdata, "md5-checksums.txt");
        try (final FileWriter writer = new FileWriter(checksums)) {
            list(grunnlagsdata)
                    .filter(file -> file.getName().contains("csv"))
                    .map(this::md5checksumline)
                    .forEach((str) -> {
                        try {
                            writer.write(str);
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<File> list(final File katalog) {
        return Optional.ofNullable(katalog.listFiles()).map(Stream::of).orElse(Stream.empty());
    }

    private String md5checksumline(final File file) {
        return format(
                "%s *%s\n",
                md5sum.produser(file),
                file.getName()
        );
    }
}
