package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

class Optionals {
    static <T> Stream<T> stream(final Optional<T> value) {
        return value.map(Stream::of).orElse(Stream.empty());
    }
}
