package no.spk.premie.tidsserie.batch.core.medlem;

import java.util.Map;

import no.spk.premie.tidsserie.batch.core.Tidsseriemodus;

/**
 * {@link MedlemsdataBackend} representerer backend-systemet som er ansvarlig for generering
 * av tidsseriar.
 * <br>
 * Backendsystemet lar klientar overføre medlemsdata til backenden for mellomlagring fram til klienten ønskjer
 * å generere ein tidsserie.
 * <br>
 * Tidsseriane som blir generert kan vere på fleire forskjellige format og nivå. Kvar tidsserietype har ei
 * separat konstruksjonsmetode nedanfor.
 * <br>
 * Før medlemsdata kan bli lasta opp til backenden og tidsseriar generert, må backenden startast opp via
 * {@link #start()}.
 * <br>
 * Det er opp til kvar backend å avgjere kva og korleis oppstart, opplasting og generering blir handtert
 * "behind-the-scenes".
 * <br>
 * Kva tidsperiode genererte tidsserien strekker seg over, står {@link Tidsseriemodus} ansvarlig for å bestemme.
 *
 * @author Tarjei Skorgenes
 */
public interface MedlemsdataBackend {
    /**
     * Startar opp og allokerer ressursar påkrevd for å motta medlemsdata frå klientar
     * for å kunne generere tidsseriar.
     */
    void start();

    /**
     * Hentar ut kommandoobjektet som lar ein laste opp medlemsdata til backenden.
     *
     * @return kommando for opplasting av medlemsdata til backenden
     */
    MedlemsdataUploader uploader();

    /**
     * Genererer ein ny tidsserie. Formatet bestemmes av {@link Tidsseriemodus}.
     *
     * @return alle meldingar som har blitt generert i løpet av tidsseriegenereringa, gruppert på melding med antall
     * gangar meldinga var generert som verdi
     */
    Map<String, Integer> lagTidsserie();

}
