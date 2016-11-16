package no.spk.pensjon.faktura.tidsserie.batch.core;

import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.medlem.BehandleMedlemCommand;
import no.spk.pensjon.faktura.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.batch.core.medlem.Medlemsbehandler;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.felles.tidsperiode.underlag.reglar.Regelsett;
import no.spk.felles.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link Tidsseriemodus} er integrasjonspunktet mellom tidsserieplatformen og den funksjonelle logikken som
 * utfører selve generering av tidsserien.
 * <br>
 * Hver modus kan anta at platformen har startet opp, alle tjenester har blitt initialisert og startet opp og at alt av
 * grunnlagsdata har blitt lest inn i minne innen {@link #lagTidsserie(ServiceRegistry)} blir kallet.
 * <br>
 * Modusen er ansvarlig for å velge om den har behov for distribuert prosessering på medlemsnivå eller om den
 * ikkje trenger å operere på medlemsdata, og dermed ikke trenger å prosessere distribuert.
 * <br>
 * Modusen er kun ansvarlig for oppbyging av underlagene og -periodene som skal lagres og serialisering av periodene
 * til CSV-linjer. Selve lagringen tas hånd om av platformen vha {@link StorageBackend}.
 * <br>
 * Modusen kan også overstyre flesteparten av standardtjenestene som platformen tilbyr, gjennom å registrere høyere
 * rangerte implementasjoner av desse tjenestene i tjenesteregisteret (via {@link #registerServices(ServiceRegistry)}.
 * De kan også plugge seg inn i livssyklushåndteringen til platformen (f.eks. via {@link TidsserieLivssyklus}).
 *
 * @author Tarjei Skorgenes
 * @see TidsserieGenerertCallback
 * @see GenererTidsserieCommand
 * @see TidsserieLivssyklus
 * @see AgentInitializer
 * @see StorageBackend
 * @see Katalog
 * @see TidsserieFactory
 * @see TidsperiodeFactory
 * @see GrunnlagsdataRepository
 */
public interface Tidsseriemodus {

    /**
     * Metoden kalles før generering av tidsserie, slik at modusen kan registrere tjenester
     * den skal benytte senere i tjenesteregisteret, eller for å overstyre tjenester levert som en del av
     * platformen.
     * <br>
     * Tjenester i tjenesteregisteret skal ikke kalles fra implementasjoner av denne metoden, da den blir kalt før
     * tjenestene er initialisert, men etter at de er registrert.
     * <br>
     * Bruk av tjenestene kan kun skje innenfor {@link #lagTidsserie(ServiceRegistry)}
     *
     * @param serviceRegistry tjenesteregistertet som nye tjenester skal registreres i
     */
    void registerServices(ServiceRegistry serviceRegistry);

    /**
     * Genererer ein ny tidsserie.
     * <br>
     *
     * @param registry tjenesteregister som benyttes for kjøringen
     * @return alle meldingar som har blitt generert i løpet av tidsseriegenereringa, gruppert på melding med antall
     * gangar meldinga var generert som verdi
     */
    Map<String, Integer> lagTidsserie(ServiceRegistry registry);

    /**
     * Eit navn som unikt identifiserer modusen for å skille den frå andre modusar.
     * <br>
     * Blir brukt for å gi brukaren muligheit til å angi ønska modus for batchkøyringa på kommandolinja.
     *
     * @return ein streng som inneheld eit modusnavn som brukaren ønskjer å generere ein tidsserie med
     * @since 2.1.0
     */
    String navn();
}
