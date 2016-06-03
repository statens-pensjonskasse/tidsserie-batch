package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractListAssert;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

class UtkatalogRule extends ExternalResource {
    private final TemporaryFolder temp;

    private Path utKatalog;

    UtkatalogRule(final TemporaryFolder temp) {
        this.temp = temp;
    }

    @Override
    protected void before() throws Throwable {
        utKatalog = temp.newFolder("ut").toPath();
    }

    void write(final String expected, final List<String> linjer) throws IOException {
        Files.write(utKatalog.resolve(expected), linjer);
    }

    Path ut() {
        return utKatalog;
    }

    AbstractListAssert<?, ? extends List<? extends Path>, Path> assertFillister() {
        try {
            return assertThat(fillister())
                    .as("fillister i ut-katalogen");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    AbstractListAssert<?, ? extends List<? extends Path>, Path> assertTidsseriefiler() {
        try {
            return assertThat(tidsseriefiler())
                    .as("tidsseriefiler i ut-katalogen");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    AbstractListAssert<?, ? extends List<? extends String>, String> assertFillisteInnhold() {
        try {
            final Path filliste = fillister().findFirst().get();
            return assertThat(Files.readAllLines(filliste))
                    .as("innhold fra fillisten " + filliste);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<Path> fillister() throws IOException {
        return alleFiler()
                .filter(p -> p.toFile().getName().startsWith("FFF_FILLISTE"))
                .filter(p -> p.toFile().getName().endsWith(".txt"));
    }

    private Stream<Path> tidsseriefiler() throws IOException {
        return alleFiler()
                .filter(p -> p.toFile().getName().startsWith("tidsserie"))
                .filter(p -> p.toFile().getName().endsWith(".csv"));
    }

    private Stream<Path> alleFiler() throws IOException {
        return realiser(Files.list(utKatalog));
    }

    private Stream<Path> realiser(final Stream<Path> stream) {
        try (final Stream<Path> filer = stream) {
            return filer.collect(toList()).stream();
        }
    }
}