package no.spk.felles.tidsserie.batch.core;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;

/**
 * {@link AgentInitializer} mottar notifikasjonar frå {@link MedlemsdataBackend}
 * kvar gang tidsseriegenereringa skal til å starte prosessering av ein ny medlemspartisjon.
 * <br>
 * Ved å plugge inn tenester av denne typen kan dermed modusane initiere initialisering eller
 * forberedelse av ressursar som deira implementasjon av {@link GenererTidsserieCommand} ønskjer
 * å kunne forutsette at har blitt gjort i forkant, f.eks. generere ei header-linje i kvar
 * output-fil før tidsseriane for medlemmane i partisjonen blir lagra fortløpande til samme fil.
 *
 * @author Snorre E. Brekke - Computas
 */
public interface AgentInitializer {
    /**
     * Notifiserer tenesta om at {@link MedlemsdataBackend} skal til å starte generering av
     * tidsseriar for medlemmer i ein bestemt partisjon.
     *
     * @param serienummer serienummeret som partisjonen har blitt tildelt, er garantert å
     * vere unikt innanfor kvar køyring av batchen
     */
    void partitionInitialized(long serienummer);
}
