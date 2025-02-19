package no.spk.premie.tidsserie.batch.main;

/**
 * Exception som kastes dersom {@link DirectoryCleaner} eller {@link DeleteBatchDirectoryFinder} feiler.
 * @author Snorre E. Brekke - Computas
 */
public class HousekeepingException extends Exception {
    private final static long serialVersionUID = 1;

    public HousekeepingException(String message) {
        super(message);
    }

    public HousekeepingException(String message, Throwable cause) {
        super(message, cause);
    }
}
