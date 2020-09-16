package no.spk.felles.tidsserie.batch.backend.hazelcast;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class HazelcastPlugin implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final HazelcastBackend backend = new HazelcastBackend(
                registry,
                antallProsessorar(locator)
        );
        registry.registerService(MedlemsdataBackend.class, backend);
        registry.registerService(TidsserieLivssyklus.class, backend);
    }

    private AntallProsessorar antallProsessorar(final ServiceLocator locator) {
        return locator
                .firstMandatory(TidsserieBatchArgumenter.class)
                .antallProsessorar()
                ;
    }
}
