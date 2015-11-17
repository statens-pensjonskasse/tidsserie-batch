package no.spk.pensjon.faktura.tidsserie.batch.main;

/**
 * @author Snorre E. Brekke - Computas
 */
public class GrunnlagsdataException extends RuntimeException {
    private final static long serialVersionUID = 1;

    public GrunnlagsdataException(String message) {
        super(message);
    }

    public GrunnlagsdataException(Throwable cause) {
        super(cause);
    }
}
