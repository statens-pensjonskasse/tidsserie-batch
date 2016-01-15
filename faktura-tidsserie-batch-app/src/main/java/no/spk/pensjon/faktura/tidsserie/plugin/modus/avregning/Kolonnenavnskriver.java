package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning;

import static java.util.stream.Collectors.joining;
import static no.spk.pensjon.faktura.tidsserie.util.Services.lookup;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * @author Snorre E. Brekke - Computas
 */
public class Kolonnenavnskriver implements TidsserieLivssyklus {

    private final List<String> kolonnenavn;

    public Kolonnenavnskriver(List<String> kolonnenavn) {
        this.kolonnenavn = kolonnenavn;
    }

    @Override
    public void start(ServiceRegistry registry) {
        final StorageBackend storage = lookup(registry, StorageBackend.class);
        storage.lagre(event -> event.buffer
                .append(kolonnenavn.stream().collect(joining(";")))
                .append('\n'));
    }
}
