package no.spk.pensjon.faktura.tidsserie.batch.upload;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link TidsserieBackendService} representerer backend-systemet som er ansvarlig for generering
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
 *
 * @author Tarjei Skorgenes
 */
public interface TidsserieBackendService {
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
     * Genererer ein ny tidsserie på stillingsforholdnivå.
     * <br>
     * Tidsserien består av {@link TidsserieObservasjon observasjonar} pr stillingsforhold, avtale, premieår og
     * observasjonsdato.
     * <br>
     * Tidsseriens observasjonsperiode strekker seg frå 1. januar i <code>fraOgMed</code>-året til 31. desember i
     * <code>tilOgMed</code>-året.
     * <br>
     * Alle resultat vil bli lagra til filer navngitt basert på <code>outputFiles</code>. Filnavnet til dette objektet
     * blir brukt som ein mal og alle forekomstar av XXX blir bytta ut med unike verdiar backenden genererer for
     * å splitte opp output-fila i fleire mindre filer av passande størrelse. Størrelse og oppsplittingsstrategi
     * er ein backend-spesifikk implementasjonsdetalj som klienten ikkje kan påvirke direkte.
     *
     * @param outputFiles mal for filnavna tidsserien skal lagrast til
     * @param fraOgMed    årstallet tidsserien skal starte i
     * @param tilOgMed    årstallet tidsserien skal avsluttast i
     * @param executors   threadpool som lagringa av observasjonar skal køyre via
     * @return alle meldingar som har blitt generert i løpet av tidsseriegenereringa, gruppert på melding med antall
     * gangar meldinga var generert som verdi
     * @see Observasjonsperiode
     */
    Map<String, Integer> lagTidsseriePaaStillingsforholdNivaa(
            FileTemplate outputFiles,
            Aarstall fraOgMed,
            Aarstall tilOgMed,
            ExecutorService executors
    );

    /**
     * Registrerer tenesta i backendens tenesteregister slik at dei blir tilgjengelig for backendens beregningsagentar.
     * <br>
     * Tenesta <code>service</code> blir registrert under tenestenavnet angitt av <code>serviceType</code>, det
     * forventast at tenesta kan castast til den angitte typen av klientane som slår opp og benyttar seg av tenesta.
     *
     * @param <T>         tenestetypen som blir registrert
     * @param serviceType kva tenestetype tenesta skal registrerast under
     * @param service     tenesta som skal registrerast under den angitte tenestetypen i backenden sitt tjenesteregister
     */
    <T> void registrer(Class<T> serviceType, T service);
}
