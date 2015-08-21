package no.spk.pensjon.faktura.tidsserie.batch.main;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TidsserieException extends RuntimeException {
    public TidsserieException(String message) {
        super(message);
    }

    public TidsserieException(Throwable cause) {
        super(cause);
    }
}
