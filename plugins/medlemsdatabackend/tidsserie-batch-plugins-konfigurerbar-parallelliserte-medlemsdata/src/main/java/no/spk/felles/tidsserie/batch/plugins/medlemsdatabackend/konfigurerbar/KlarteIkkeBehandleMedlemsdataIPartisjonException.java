package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;

import java.util.Objects;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;

class KlarteIkkeBehandleMedlemsdataIPartisjonException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final Partisjonsnummer partisjonsnummer;

    public KlarteIkkeBehandleMedlemsdataIPartisjonException(Partisjonsnummer partisjonsnummer, Exception e) {
        super(e);
        this.partisjonsnummer = Objects.requireNonNull(partisjonsnummer, "partisjonsnummer er påkrevd, men var null");
    }

    @Override
    public String getMessage() {
        return format("Klarte ikke behandle medlemsdata som kom inn til partisjon %s", partisjonsnummer);
    }
}
