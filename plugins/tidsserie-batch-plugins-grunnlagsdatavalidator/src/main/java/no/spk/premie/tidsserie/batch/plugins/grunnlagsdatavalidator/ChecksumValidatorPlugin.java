package no.spk.premie.tidsserie.batch.plugins.grunnlagsdatavalidator;

import java.nio.file.Path;

import no.spk.premie.tidsserie.batch.core.Katalog;
import no.spk.premie.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;
import no.spk.premie.tidsserie.batch.core.registry.Plugin;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class ChecksumValidatorPlugin implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);
        final Path innKatalog = locator.firstMandatory(Path.class, Katalog.GRUNNLAGSDATA.egenskap());
        registry.registerService(UttrekksValidator.class, new ChecksumValideringAvGrunnlagsdata(innKatalog));
    }
}
