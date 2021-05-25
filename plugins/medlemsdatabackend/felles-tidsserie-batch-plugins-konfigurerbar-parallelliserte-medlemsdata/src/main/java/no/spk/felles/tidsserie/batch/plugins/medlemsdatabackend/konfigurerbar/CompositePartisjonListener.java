package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
