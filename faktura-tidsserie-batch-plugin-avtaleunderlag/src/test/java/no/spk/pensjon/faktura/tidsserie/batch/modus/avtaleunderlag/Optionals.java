package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import java.util.Optional;
import java.util.stream.Stream;

class Optionals {
    static <T> Stream<T> stream(final Optional<T> value) {
        return value.map(Stream::of).orElse(Stream.empty());
    }
}
