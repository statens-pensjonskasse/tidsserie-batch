package no.spk.tidsserie.batch.core;

import java.util.function.Function;
import java.util.function.Predicate;

public class Validators {
    public static <T, E extends RuntimeException> T require(final T value, final Predicate<T> validator, final Function<T, E> feilmelding) {
        if (!validator.test(value)) {
            throw feilmelding.apply(value);
        }
        return value;
    }
}
