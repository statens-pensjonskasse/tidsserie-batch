package no.spk.pensjon.faktura.tidsserie.batch;

import java.io.File;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link TidsserieBackendService} representerer backend-systemet som er ansvarlig for generering
 * av tidsseriar.
 * <br>
 * Backendsystemet lar klientar overf�re medlemsdata til backenden for mellomlagring fram til klienten �nskjer
 * � generere ein tidsserie.
 * <br>
 * Tidsseriane som blir generert kan vere p� fleire forskjellige format og niv�. Kvar tidsserietype har ei
 * separat konstruksjonsmetode nedanfor.
 * <br>
 * F�r medlemsdata kan bli lasta opp til backenden og tidsseriar generert, m� backenden startast opp via
 * {@link #start()}.
 * <br>
 * Det er opp til kvar backend � avgjere kva og korleis oppstart, opplasting og generering blir handtert
 * "behind-the-scenes".
 *
 * @author Tarjei Skorgenes
 */
public interface TidsserieBackendService {
    /**
     * Startar opp og allokerer ressursar p�krevd for � motta medlemsdata fr� klientar
     * for � kunne generere tidsseriar.
     */
    void start();

    /**
     * Hentar ut kommandoobjektet som lar ein laste opp medlemsdata til backenden.
     *
     * @return kommando for opplasting av medlemsdata til backenden
     */
    MedlemsdataUploader uploader();

    /**
     * Genererer ein ny tidsserie p� stillingsforholdniv�.
     * <br>
     * Tidsserien best�r av {@link TidsserieObservasjon observasjonar} pr stillingsforhold, avtale, premie�r og
     * observasjonsdato.
     * <br>
     * Tidsseriens observasjonsperiode strekker seg fr� 1. januar i <code>fraOgMed</code>-�ret til 31. desember i
     * <code>tilOgMed</code>-�ret.
     * <br>
     * Alle resultat vil bli lagra til filer navngitt basert p� <code>outputFiles</code>. Filnavnet til dette objektet
     * blir brukt som ein mal og alle forekomstar av XXX blir bytta ut med unike verdiar backenden genererer for
     * � splitte opp output-fila i fleire mindre filer av passande st�rrelse. St�rrelse og oppsplittingsstrategi
     * er ein backend-spesifikk implementasjonsdetalj som klienten ikkje kan p�virke direkte.
     *
     * @param outputFiles mal for filnavna tidsserien skal lagrast til
     * @param fraOgMed    �rstallet tidsserien skal starte i
     * @param tilOgMed    �rstallet tidsserien skal avsluttast i
     * @return alle meldingar som har blitt generert i l�pet av tidsseriegenereringa, gruppert p� melding med antall
     * gangar meldinga var generert som verdi
     * @see Observasjonsperiode
     */
    Map<String, Integer> lagTidsseriePaaStillingsforholdNivaa(
            File outputFiles,
            Aarstall fraOgMed,
            Aarstall tilOgMed
    );
}
