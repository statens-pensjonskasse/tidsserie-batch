package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
