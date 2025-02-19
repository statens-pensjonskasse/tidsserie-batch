package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public class SkalertBufferDatalagringStrategi implements DatalagringStrategi {

    @Override
    public Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return SkalertBufferMedlemsdata.medlemsdata(medlemsdata);
    }
}
