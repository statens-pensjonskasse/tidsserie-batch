package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;

import java.util.Objects;

import no.spk.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

class KlarteIkkeLeseMedlemsdataIPartisjonException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final Partisjonsnummer partisjonsnummer;

    public KlarteIkkeLeseMedlemsdataIPartisjonException(Partisjonsnummer partisjonsnummer, Exception e) {
        super(e);
        this.partisjonsnummer = Objects.requireNonNull(partisjonsnummer, "partisjonsnummer er påkrevd, men var null");
    }

    @Override
    public String getMessage() {
        return format("Klarte ikke lese medlemsdata fra partisjon %s", partisjonsnummer);
    }
}
