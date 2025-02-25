package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;


public interface Medlemsdata {

    byte[] medlemsdata();

    Medlemsdata put(Medlemsdata innData);
}
