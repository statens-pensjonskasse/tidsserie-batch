package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class KomprimertDatalagringStrategi implements DatalagringStrategi {

    @Override
    public Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return KomprimertMedlemsdata.medlemsdata(medlemsdata);
    }
}
