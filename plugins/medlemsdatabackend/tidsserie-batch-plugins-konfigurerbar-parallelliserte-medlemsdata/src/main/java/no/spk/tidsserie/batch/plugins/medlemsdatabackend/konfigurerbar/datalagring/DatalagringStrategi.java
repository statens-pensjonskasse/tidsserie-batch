package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

public interface DatalagringStrategi {

    Medlemsdata medlemsdata(byte[] medlemsdata);
}
