package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;

public class SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final String verdi;

    public SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException(final String verdi) {
        this.verdi = requireNonNull(verdi, "verdi er påkrevd, men var null");
    }
}