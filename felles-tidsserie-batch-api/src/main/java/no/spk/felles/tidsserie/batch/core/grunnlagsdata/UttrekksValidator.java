package no.spk.felles.tidsserie.batch.core.grunnlagsdata;

/**
 * {@link UttrekksValidator} er ansvarlig for å validere at grunnlagsdatane i uttrekket
 * som batchen har blitt angitt til å bruke, er gyldige.
 *
 * @author Snorre E. Brekke - Computas
 * @since 1.1.0
 */
public interface UttrekksValidator {
    void validate() throws UgyldigUttrekkException;
}
