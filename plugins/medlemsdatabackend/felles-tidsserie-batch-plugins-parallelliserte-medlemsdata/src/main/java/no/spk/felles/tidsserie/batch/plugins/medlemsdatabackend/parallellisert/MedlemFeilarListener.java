package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

interface MedlemFeilarListener {
    void medlemFeila(String medlemsId, final Throwable t);
}
