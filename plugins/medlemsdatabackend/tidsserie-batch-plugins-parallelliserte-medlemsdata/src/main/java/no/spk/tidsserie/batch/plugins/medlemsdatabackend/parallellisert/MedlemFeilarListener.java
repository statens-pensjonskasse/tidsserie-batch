package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

interface MedlemFeilarListener {
    void medlemFeila(String medlemsId, final Throwable t);
}
