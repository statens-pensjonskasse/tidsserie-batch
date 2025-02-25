package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

public class MedlemsdataBuilder {
    private static final String DELIMITER_COLUMN = ";";
    private static final String DELIMITER_ROW = "\n";

    @SafeVarargs
    @SuppressWarnings("varargs")
    static Medlemsdata medlemsdata(final List<String>... rader) {
        return new DefaultDatalagringStrategi().medlemsdata(
                Arrays.stream(rader)
                        .map(row -> join(DELIMITER_COLUMN, row))
                        .collect(joining(DELIMITER_ROW))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    static List<String> rad(final String... celle) {
        return Arrays.asList(celle);
    }
}
