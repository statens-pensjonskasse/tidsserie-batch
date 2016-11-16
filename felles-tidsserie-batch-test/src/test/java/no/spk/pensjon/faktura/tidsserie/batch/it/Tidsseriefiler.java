package no.spk.pensjon.faktura.tidsserie.batch.it;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cucumber.api.DataTable;

/**
 * {@link Tidsseriefiler} leser inn alle *.csv-filer som ligger i tidsserie-katalogen og gjør resultatet tilgjengelig for verifisering.
 */
class Tidsseriefiler {
    private List<String> ignorerteKolonner = new ArrayList<>();

    private final File utKatalog;

    Tidsseriefiler(final File utKatalog) {
        this.utKatalog = utKatalog;
    }

    void ignorerKolonner(final List<String> kolonner) {
        this.ignorerteKolonner.addAll(kolonner);
    }


    DataTable konverterTidsserierTilTabell(final CSVFiler kilde) {
        try (final Stream<Path> files = tidsseriefiler()) {
            final List<String> alleLinjer = kilde.lesAlleLinjerFra(files);
            final List<String> kolonnenavn = finnKolonnenavn(alleLinjer);
            return tilTabellUtenIgnorerteKolonner(
                    kolonnenavn,
                    finnTidsserielinjer(
                            alleLinjer,
                            kolonnenavn
                    )
            );
        }
    }

    private Predicate<String> kunKolonnerSomIkkjeErIgnorert() {
        return not(ignorerteKolonner::contains);
    }

    private static <T> Predicate<T> not(final Predicate<T> contains) {
        return contains.negate();
    }

    private Stream<Path> tidsseriefiler() {
        try {
            return Files.list(utKatalog.toPath().resolve("tidsserie"))
                    .filter(this::erTidsseriefil);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean erTidsseriefil(Path p) {
        return p.toFile().getName().endsWith(".csv") && p.toFile().getName().startsWith("tidsserie");
    }

    private DataTable tilTabellUtenIgnorerteKolonner(List<String> kolonnenavn, List<List<String>> underlagsperiodeLinjer) {
        return DataTable.create(
                Stream.concat(
                        singletonList(
                                kolonnenavn
                                        .stream()
                                        .filter(kunKolonnerSomIkkjeErIgnorert())
                                        .collect(toList())
                        )
                                .stream(),
                        underlagsperiodeLinjer.stream()
                )
                        .collect(Collectors.toList())
        );
    }

    private List<List<String>> finnTidsserielinjer(final List<String> lines, final List<String> kolonner) {
        return lines
                .stream()
                .filter(not(isHeaderLine()))
                .map(line -> line.split(";", -1))
                .map(Arrays::asList)
                .map((felt) -> fjernIgnorerteKolonner(felt, kolonner))
                .collect(toList());
    }

    private List<String> finnKolonnenavn(final List<String> lines) {
        final Supplier<Error> headerlinjeManglar = this::headerlinjeManglar;
        return lines
                .stream()
                .filter(isHeaderLine())
                .distinct()
                .reduce((a, b) -> {
                    throw new AssertionError(
                            "Fleire headerlinjer med ulikt innhold har blitt lokalisert:\n"
                                    + "Kolonnelinje a:\n\t" + a
                                    + "Kolonnelinje b:\n\t" + b
                    );
                })
                .map(header -> header.split(";"))
                .map(Arrays::asList)
                .orElseThrow(headerlinjeManglar);
    }

    private List<String> fjernIgnorerteKolonner(final List<String> felt, final List<String> kolonner) {
        return kolonner
                .stream()
                .filter(kunKolonnerSomIkkjeErIgnorert())
                .map(kolonner::indexOf)
                .map(felt::get)
                .collect(toList());
    }

    private Error headerlinjeManglar() {
        return new AssertionError("Ingen tidsseriefiler med ein headerlinje har blitt lokalisert under " + this.utKatalog);
    }

    private Predicate<String> isHeaderLine() {
        return line -> !line.matches("^[0-9]+.+");
    }
}
