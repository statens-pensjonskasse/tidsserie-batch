package no.spk.pensjon.faktura.tidsserie.storage.main;

/**
 * @author Snorre E. Brekke - Computas
 */
public class GrunnlagsdataException extends RuntimeException {
    public GrunnlagsdataException() {
    }

    public GrunnlagsdataException(String message) {
        super(message);
    }

    public GrunnlagsdataException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrunnlagsdataException(Throwable cause) {
        super(cause);
    }

    public GrunnlagsdataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
