package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
