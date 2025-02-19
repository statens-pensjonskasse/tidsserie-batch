package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

interface CompositePartisjonListener {
    void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
}
