package no.spk.felles.tidsserie.batch.core.medlem;

import java.util.List;

import no.spk.felles.tidsserie.batch.core.lagring.ObservasjonsEvent;

/**
 * {@link TidsserieContext} er bindeleddet mellom {@link GenererTidsserieCommand} og teknologien
 * som utfører distribuert generering av medlemsrelaterte tidsseriar.
 * <br>
 * Via denne kan kommandoane notifisere det distribuerte systemet om feil som oppstår eller
 * be om informasjon som krever distribuert koordinering for å utlede (f.eks. serienummer).
 */
public interface TidsserieContext {
    /**
     * Publiserer informasjon om ein feil som har oppstått
     * under generering av tidsserien til eit medlem.
     * <br>
     * Medlemsdatabackenden er ansvarlig for å ta vare på aggregert informasjon
     * utleda frå feilen, for ved terminering av batchen å kunne informere om
     * antall feil som har avbrutt tidsseriegenereringa for eit eller fleire
     * medlemmar og/eller stillingsforhold.
     *
     * @param throwable feilen som har oppstått
     */
    void emitError(Throwable throwable);

    /**
     * Eit serienummer som gir ein grov indikasjon på antall partisjonar
     * som medlemmet som noverande kall til
     * {@link GenererTidsserieCommand#generer(String, List, TidsserieContext)}
     * tilhøyrer.
     * <br>
     * Serienummeret og den relaterte partisjoneringa er ikkje garantert å vere
     * deterministisk på tvers av køyringar av batchen, det bør primært
     * benyttast av modusar som ønskjer å lagre tidsserien på ein måte som sprer
     * den over fleire filer, men der alle rader tilknytta eit bestemt medlem er
     * garantert å ende opp ei og samme fil.
     *
     * @return eit serienummer som indikerer kva partisjon medlemmet tilhøyrer i
     * den distribuerte medlemsdatabackenden
     * @see ObservasjonsEvent#serienummer()
     */
    long getSerienummer();
}
