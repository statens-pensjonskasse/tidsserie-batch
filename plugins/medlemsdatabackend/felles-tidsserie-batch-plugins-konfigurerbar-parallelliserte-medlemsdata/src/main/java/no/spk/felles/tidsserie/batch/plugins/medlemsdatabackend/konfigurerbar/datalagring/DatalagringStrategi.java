package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public interface DatalagringStrategi {

    Medlemsdata medlemsdata(byte[] medlemsdata);
}
