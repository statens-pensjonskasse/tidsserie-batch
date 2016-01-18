package no.spk.pensjon.faktura.tidsserie.plugin.modus;

import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.List;

import no.spk.pensjon.faktura.tidsserie.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;

/**
 * @author Snorre E. Brekke - Computas
 */
public class KolonnenavnPerPartisjon implements AgentInitializer {

    private final StorageBackend storage;
    private final List<String> kolonnenavn;

    public KolonnenavnPerPartisjon(List<String> kolonnenavn, StorageBackend storage) {
        this.storage = storage;
        this.kolonnenavn = Collections.unmodifiableList(kolonnenavn);
    }

    @Override
    public void partitionInitialized(long serienummer) {
        storage.lagre(event -> event.serienummer(serienummer)
                .buffer
                .append(kolonnenavn.stream().collect(joining(";")))
                .append('\n')
        );
    }
}
