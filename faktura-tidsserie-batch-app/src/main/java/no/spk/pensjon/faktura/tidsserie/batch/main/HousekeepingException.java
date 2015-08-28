package no.spk.pensjon.faktura.tidsserie.batch.main;

/**
 * Exception som kastes dersom {@link DirectoryCleaner} eller {@link DeleteBatchDirectoryFinder} feiler.
 * @author Snorre E. Brekke - Computas
 */
public class HousekeepingException extends Exception {

    public HousekeepingException(String message) {
        super(message);
    }

    public HousekeepingException(String message, Throwable cause) {
        super(message, cause);
    }
}
