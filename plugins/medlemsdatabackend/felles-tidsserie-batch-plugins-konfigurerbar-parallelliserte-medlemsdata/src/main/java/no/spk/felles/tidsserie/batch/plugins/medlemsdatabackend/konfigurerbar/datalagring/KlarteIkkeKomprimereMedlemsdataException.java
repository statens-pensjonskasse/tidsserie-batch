package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class KlarteIkkeKomprimereMedlemsdataException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public KlarteIkkeKomprimereMedlemsdataException(final String message) {
        super(message);
    }
}
