package no.spk.premie.tidsserie.batch.plugins.metadatawriter;

import java.nio.file.Path;

import no.spk.premie.tidsserie.batch.core.Katalog;
import no.spk.premie.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.premie.tidsserie.batch.core.registry.Plugin;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class MetadataWriterPlugin implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final Path logKatalog = locator.firstMandatory(Path.class, Katalog.LOG.egenskap());
        final TidsserieBatchArgumenter argumenter = locator.firstMandatory(TidsserieBatchArgumenter.class);
        registry.registerService(
                TidsserieGenerertCallback2.class,
                new LagreMetadata(logKatalog, argumenter)
        );
    }
}
