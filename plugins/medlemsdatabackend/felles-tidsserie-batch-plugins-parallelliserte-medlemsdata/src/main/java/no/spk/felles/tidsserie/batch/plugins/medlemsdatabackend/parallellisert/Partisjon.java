package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;

class Partisjon {
    private static final String DELIMITER_COLUMN = ";";

    private static final String DELIMITER_ROW = "\n";

    private static final byte[] DELIMITER_ROW_BYTES = "\n".getBytes(StandardCharsets.UTF_8);

    private final LinkedHashMap<String, byte[]> medlemsdata = new LinkedHashMap<>();

    private final Partisjonsnummer nummer;

    Partisjon(final Partisjonsnummer nummer) {
        this.nummer = requireNonNull(nummer, "nummer er påkrevd, men var null");
    }

    void put(final String key, final List<List<String>> value) {
        this.medlemsdata.merge(
                key,
                value
                        .stream()
                        .peek(this::valider)
                        .map(row -> join(DELIMITER_COLUMN, row))
                        .collect(joining(DELIMITER_ROW))
                        .getBytes(StandardCharsets.UTF_8),
                this::append
        );
    }

    void forEach(final BiConsumer<String, List<List<String>>> consumer) {
        medlemsdata.forEach(
                (key, bytes) -> consumer.accept(
                        key,
                        stream(
                                new String(
                                        bytes,
                                        StandardCharsets.UTF_8
                                )
                                        .split("\n")
                        )
                                .map(
                                        row -> asList(
                                                row.split(
                                                        DELIMITER_COLUMN,
                                                        ikkjeIgnorerTommeKolonnerPåSluttenAvRada()
                                                )
                                        )
                                )
                                .collect(toList())
                )
        );
    }

    Partisjonsnummer nummer() {
        return nummer;
    }

    boolean isEmpty() {
        return medlemsdata.isEmpty();
    }

    int size() {
        return medlemsdata.size();
    }

    @Override
    public String toString() {
        return format(
                "%s (%d medlemmar)",
                nummer.toString(),
                medlemsdata.keySet().size()
        );
    }

    private int ikkjeIgnorerTommeKolonnerPåSluttenAvRada() {
        return -1;
    }

    private byte[] append(final byte[] forrige, final byte[] neste) {
        final byte[] til = new byte[forrige.length + DELIMITER_ROW_BYTES.length + neste.length];

        kopier(forrige, /*       */ til, 0);
        kopier(DELIMITER_ROW_BYTES, til, forrige.length);
        kopier(neste, /*         */ til, forrige.length + DELIMITER_ROW_BYTES.length);

        return til;
    }

    private void kopier(final byte[] fra, final byte[] til, final int tilIndex) {
        System.arraycopy(fra, 0, til, tilIndex, fra.length);
    }

    private void valider(final List<String> rad) {
        rad.forEach(this::valider);
    }

    private void valider(final String verdi) {
        if (verdi.contains(DELIMITER_COLUMN)) {
            throw new SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException(verdi);
        }

        if (verdi.contains(DELIMITER_ROW)) {
            throw new LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException(verdi);
        }
    }
}
