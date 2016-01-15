package no.spk.pensjon.faktura.tidsserie.plugin.modus;

import static no.spk.pensjon.faktura.tidsserie.util.Services.lookup;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DefaultTidsseriemodusLivssyklus implements TidsserieLivssyklus {

    private final List<String> kolonnenavn;

    public DefaultTidsseriemodusLivssyklus(List<String> kolonnenavn) {
        this.kolonnenavn = kolonnenavn;
    }

    @Override
    public void start(ServiceRegistry serviceRegistry) {
        final StorageBackend storageBackend = lookup(serviceRegistry, StorageBackend.class);
        serviceRegistry.registerService(AgentInitializer.class, new KolonnenavnPerPartisjon(kolonnenavn, storageBackend));
    }
}
