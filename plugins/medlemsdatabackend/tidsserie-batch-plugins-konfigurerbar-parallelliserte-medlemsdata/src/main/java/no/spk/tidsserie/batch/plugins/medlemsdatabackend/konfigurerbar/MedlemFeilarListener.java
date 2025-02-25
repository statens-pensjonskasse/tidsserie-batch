package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

interface MedlemFeilarListener {
    void medlemFeila(String medlemsId, final Throwable t);
}
