package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static java.util.Objects.requireNonNull;

/**
 * @since 1.1.0
 */
public class BruksveiledningSkalVisesException extends RuntimeException {
    private static final long serialVersionUID = 0L;
    private final Bruksveiledning bruksveiledning;

    public BruksveiledningSkalVisesException(final Bruksveiledning bruksveiledning) {
        this.bruksveiledning = requireNonNull(bruksveiledning, "bruksveiledning er påkrevd, men var null");
    }

    public String bruksveiledning() {
        return bruksveiledning.vis();
    }
}