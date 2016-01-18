package no.spk.pensjon.faktura.tidsserie.util;

import java.util.Optional;
import java.util.stream.Stream;

public final class Optionals {
    private Optionals(){
        //skal ikke instansieres
    }

    public static <T> Stream<T> stream(final Optional<T> o) {
        return o.map(Stream::of).orElse(Stream.empty());
    }
}
