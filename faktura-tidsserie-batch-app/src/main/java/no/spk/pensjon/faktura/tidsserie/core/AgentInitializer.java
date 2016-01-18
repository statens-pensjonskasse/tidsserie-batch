package no.spk.pensjon.faktura.tidsserie.core;

/**
 * {@link AgentInitializer} er en tjenestetype som benyttes som callback for å
 * kunne agere på serienummeret som tildeles en partisjon når tidsserien genereres distribuert.
 * @author Snorre E. Brekke - Computas
 */
public interface AgentInitializer {
    /**
     * Serienummeret som gjeldende pertisjoen ble tildelt.
     * @param serienummer serienummeret til partisjonen
     */
    void partitionInitialized(long serienummer);
}
