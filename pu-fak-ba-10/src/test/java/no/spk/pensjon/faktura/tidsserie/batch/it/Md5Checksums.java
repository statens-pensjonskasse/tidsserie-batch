package no.spk.pensjon.faktura.tidsserie.batch.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.DigestUtils.md5DigestAsHex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Stream;

class Md5Checksums {
    void generer(final File grunnlagsdata) {
        final File checksums = new File(grunnlagsdata, "md5-checksums.txt");
        try (final FileWriter writer = new FileWriter(checksums)) {
            list(grunnlagsdata)
                    .filter(file -> file.getName().endsWith(".gz"))
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
        try {
            try (final FileInputStream input = new FileInputStream(file)) {
                return md5DigestAsHex(input)
                        + " *"
                        + file.getName()
                        + "\n";
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
