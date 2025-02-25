package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import java.util.Optional;

import no.spk.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DatalagringStrategi;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

class DatalagringStrategiWrapper implements DatalagringStrategi {

    private final ServiceLocator locator;
    private Optional<DatalagringStrategi> datalagringStrategi = Optional.empty();

    DatalagringStrategiWrapper(final ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public Medlemsdata medlemsdata(final byte[] medlemsdata) {
        DatalagringStrategi datalagringStrategi = this.datalagringStrategi.orElse(
                locator.firstService(DatalagringStrategi.class)
                        .orElse(new DefaultDatalagringStrategi())
        );
        this.datalagringStrategi = Optional.of(datalagringStrategi);

        return datalagringStrategi.medlemsdata(medlemsdata);
    }
}
