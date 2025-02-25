package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import no.spk.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
