package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class KlarteIkkeDekomprimereMedlemsdataException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public KlarteIkkeDekomprimereMedlemsdataException(final String message) {
        super(message);
    }
}
