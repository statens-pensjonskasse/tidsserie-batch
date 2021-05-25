package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

class Partisjon {
    private static final String DELIMITER_ROW = "\n";
    private static final String DELIMITER_COLUMN = ";";

    private final LinkedHashMap<String, Medlemsdata> medlemsdata = new LinkedHashMap<>();

    private final Partisjonsnummer nummer;

    Partisjon(final Partisjonsnummer nummer) {
        this.nummer = requireNonNull(nummer, "nummer er påkrevd, men var null");
    }

    void put(final String key, final Medlemsdata medlemsdata) {
        this.medlemsdata.merge(
                key,
                medlemsdata,
                Medlemsdata::put
        );

    }

    Optional<List<List<String>>> get(final String medlemsId) {
        return Optional
                .ofNullable(medlemsdata.get(medlemsId))
                .map(this::somMedlemsdata);
    }

    void forEach(final BiConsumer<String, List<List<String>>> consumer) {
        medlemsdata.forEach(
                (key, bytes) -> consumer.accept(
                        key,
                        somMedlemsdata(bytes)
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

    private List<List<String>> somMedlemsdata(final Medlemsdata medlemsdata) {
        return stream(
                new String(
                        medlemsdata.medlemsdata(),
                        StandardCharsets.UTF_8
                )
                        .split(DELIMITER_ROW)
        )
                .map(
                        row -> asList(
                                row.split(
                                        DELIMITER_COLUMN,
                                        ikkjeIgnorerTommeKolonnerPåSluttenAvRada()
                                )
                        )
                )
                .collect(toList());
    }
}
