package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class KlarteIkkeDekomprimereMedlemsdataException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public KlarteIkkeDekomprimereMedlemsdataException(final String message) {
        super(message);
    }
}
