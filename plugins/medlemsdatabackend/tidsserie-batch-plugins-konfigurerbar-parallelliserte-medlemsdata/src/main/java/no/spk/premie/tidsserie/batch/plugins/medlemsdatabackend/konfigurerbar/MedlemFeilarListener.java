package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

interface MedlemFeilarListener {
    void medlemFeila(String medlemsId, final Throwable t);
}
