package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
