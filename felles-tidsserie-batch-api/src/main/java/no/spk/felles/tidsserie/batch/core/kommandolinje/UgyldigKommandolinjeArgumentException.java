package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static java.util.Objects.requireNonNull;

/**
 * @since 1.1.0
 */
public class UgyldigKommandolinjeArgumentException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final Bruksveiledning bruksveiledning;
    private final String message;

    public UgyldigKommandolinjeArgumentException(final String message, final Bruksveiledning bruksveiledning) {
        this.message = requireNonNull(message, "message er påkrevd, men var null");
        this.bruksveiledning = requireNonNull(bruksveiledning, "bruksveiledning er påkrevd, men var null");
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String bruksveiledning() {
        return bruksveiledning.vis();
    }
}