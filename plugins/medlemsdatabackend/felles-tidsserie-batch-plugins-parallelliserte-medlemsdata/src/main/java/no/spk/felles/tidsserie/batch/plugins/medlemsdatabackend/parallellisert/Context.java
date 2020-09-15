package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.lang.String.format;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.medlem.TidsserieContext;

class Context implements TidsserieContext {
    private final LinkedHashMap<String, Integer> messages = new LinkedHashMap<>();

    private final int serienummer;

    Context(final Partisjonsnummer nummer) {
        this.serienummer = Math.toIntExact(nummer.index() + 1);
    }

    @Override
    public long getSerienummer() {
        return serienummer;
    }

    @Override
    public void emitError(final Throwable t) {
        emit("errors");
        emit("errors_type_" + t.getClass().getSimpleName());
        emit("errors_message_" + (t.getMessage() != null ? t.getMessage() : "null"));
    }

    @Override
    public String toString() {
        return format("Context[serienummer=%d]", serienummer);
    }

    void emit(final String key) {
        messages.merge(
                key,
                1,
                Integer::sum
        );
    }

    Map<String, Integer> toMap() {
        return Collections.unmodifiableMap(messages);
    }

    void inkluderFeilmeldingarFrå(final Runnable handling) {
        try {
            handling.run();
        } catch (final RuntimeException e) {
            this.emitError(e);
        }
    }
}
