package no.spk.pensjon.faktura.tidsserie.batch.main;

/**
 * @author Snorre E. Brekke - Computas
 */
public class CleanBatchError {
    private final String label;
    private final Exception exception;

    public CleanBatchError(String label, Exception exception) {
        this.label = label;
        this.exception = exception;
    }

    public String getLabel() {
        return label;
    }

    public Exception getException() {
        return exception;
    }
}
