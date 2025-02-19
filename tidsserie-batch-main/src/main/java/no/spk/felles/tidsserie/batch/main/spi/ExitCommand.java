package no.spk.felles.tidsserie.batch.main.spi;

/**
 * {@link ExitCommand} er tenesta som vil bli benytta for å angi exit-statusen for køyringa.
 * og terminere JVMen batchen køyrer via.
 *
 * @since 1.1.0
 */
public interface ExitCommand {
    /**
     * Termine
     *
     * @param exitCode returkoda frå batchkøringa
     */
    void exit(int exitCode);
}
