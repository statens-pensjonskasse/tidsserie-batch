package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.util.Optional;
import java.util.stream.Stream;

class Optionals {
    static <T> Stream<T> stream(final Optional<T> o) {
        return o.map(Stream::of).orElse(Stream.empty());
    }
}
