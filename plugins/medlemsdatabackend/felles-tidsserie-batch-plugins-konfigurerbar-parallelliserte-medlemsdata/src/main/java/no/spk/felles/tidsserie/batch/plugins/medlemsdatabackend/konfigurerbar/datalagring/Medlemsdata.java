package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;


public interface Medlemsdata {

    byte[] medlemsdata();

    Medlemsdata put(Medlemsdata innData);
}
