package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class DefaultDatalagringStrategi implements DatalagringStrategi {

    @Override
    public Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return DefaultMedlemsdata.medlemsdata(medlemsdata);
    }
}
