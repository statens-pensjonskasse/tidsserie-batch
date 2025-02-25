package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import java.util.Arrays;
import java.util.List;

class MedlemsdataBuilder {
    @SafeVarargs
    @SuppressWarnings("varargs")
    static List<List<String>> medlemsdata(final List<String>... rader) {
        return Arrays.asList(rader);
    }

    static List<String> rad(final String... celle) {
        return Arrays.asList(celle);
    }
}
