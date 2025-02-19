package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

interface MedlemFeilarListener {
    void medlemFeila(String medlemsId, final Throwable t);
}
