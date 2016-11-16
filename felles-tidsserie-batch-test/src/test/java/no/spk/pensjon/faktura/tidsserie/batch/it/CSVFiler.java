package no.spk.pensjon.faktura.tidsserie.batch.it;

import static java.util.stream.Collectors.toList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import cucumber.api.DataTable;

/**
 * Lagring og lesing til og fra CSV-filer på komprimert og ukomprimert format.
 */
class CSVFiler {
    void lagreLinjerTilFil(final DataTable source, final Path destination) {
        try {
            try (final GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(destination.toFile()))) {
                source.asList(String.class)
                        .forEach(line -> {
                            try {
                                output.write(line.getBytes());
                                output.write('\n');
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<String> lesAlleLinjerFra(final Stream<Path> files) {
        return files
                .map(p -> {
                    try {
                        return Files.readAllLines(p, Charset.defaultCharset());
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(List::stream)
                .collect(toList());
    }
}
