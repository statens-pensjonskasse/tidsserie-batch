package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Meldingar {
    private final Map<String, Integer> messages = new HashMap<>();

    Meldingar() {
    }

    private Meldingar(final Map<String, Integer> meldingar) {
        this.messages.putAll(meldingar);
    }

    void emit(final String key) {
        messages.merge(
                key,
                1,
                Integer::sum
        );
    }

    void emitError(final Throwable t) {
        emit("errors");
        emit("errors_type_" + t.getClass().getSimpleName());
        emit("errors_message_" + (t.getMessage() != null ? t.getMessage() : "null"));
    }

    Map<String, Integer> toMap() {
        return Collections.unmodifiableMap(messages);
    }

    Meldingar merge(final Meldingar that) {
        return new Meldingar(
                Stream.concat(
                        this.messages.entrySet().stream(),
                        that.messages.entrySet().stream()
                )
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        Integer::sum
                                )
                        )
        );
    }
}
