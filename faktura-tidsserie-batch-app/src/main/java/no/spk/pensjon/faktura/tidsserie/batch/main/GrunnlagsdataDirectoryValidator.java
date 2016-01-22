package no.spk.pensjon.faktura.tidsserie.batch.main;

/**
 * {@link GrunnlagsdataDirectoryValidator} er ansvarlig for å validere at grunnlagsdatane som
 * batchen har blitt angitt til å bruke, er gyldige.
 *
 * @author Snorre E. Brekke - Computas
 */
public interface GrunnlagsdataDirectoryValidator {
    void validate() throws GrunnlagsdataException;
}
