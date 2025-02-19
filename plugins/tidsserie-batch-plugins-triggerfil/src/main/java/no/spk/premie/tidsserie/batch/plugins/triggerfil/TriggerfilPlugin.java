package no.spk.premie.tidsserie.batch.plugins.triggerfil;

import static no.spk.premie.tidsserie.batch.core.registry.Ranking.ranking;

import java.nio.file.Path;

import no.spk.premie.tidsserie.batch.core.Katalog;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.premie.tidsserie.batch.core.registry.Plugin;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class TriggerfilPlugin implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);
        final Path utKatalog = locator.firstMandatory(Path.class, Katalog.UT.egenskap());
        registry.registerService(
                TidsserieGenerertCallback2.class,
                new TriggerfileCreator(utKatalog),
                ranking(1000).egenskap()
        );
    }
}
