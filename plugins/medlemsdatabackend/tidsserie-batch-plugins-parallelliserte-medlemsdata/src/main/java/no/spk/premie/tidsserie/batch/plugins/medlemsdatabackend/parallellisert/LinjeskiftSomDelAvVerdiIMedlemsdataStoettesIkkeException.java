package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;

public class LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final String verdi;

    public LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException(final String verdi) {
        this.verdi = requireNonNull(verdi, "verdi er påkrevd, men var null");
    }
}
