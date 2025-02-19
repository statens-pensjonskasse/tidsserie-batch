package no.spk.premie.tidsserie.batch.main;

import java.nio.file.Path;
import java.util.Map;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.premie.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.premie.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

/**
 * {@link View} er ansvarlig for presentasjon av all informasjon som brukaren
 * skal få sjå i sanntid medan batchen køyrer.
 * <p>
 * Alt av informasjons- og feilmeldingar blir vist til brukaren via viewet.
 */
public interface View {

    void startarBackend() ;

    void startarOpplasting() ;

    void opplastingFullfoert() ;

    void startarTidsseriegenerering();

    /**
     * Viser informasjon om kva kommandolinjeargument batchen støttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det støttar.
     *
     * @param e hjelp-forespørslen som inneheld informasjon om tilgjengelige argument
     */
    void visHjelp(BruksveiledningSkalVisesException e);

    /**
     * Notifiserer brukaren om at eit av kommandolinjeargumenta som er angitt, er ugyldig med
     * informasjon om kva som er feil.
     *
     * @param e valideringsfeilen som inneheld informasjon om kva som er feil med argumentet
     */
    void informerOmUgyldigKommandolinjeArgument(UgyldigKommandolinjeArgumentException e);

    void informerOmOppstart(TidsserieBatchArgumenter arguments) ;


    /**
     * Informerer brukaren om at batchen har køyrt ferdig og at køyringa gjekk føre seg utan nokon feil.
     *
     * @param arbeidskatalog den nye arbeidskatalogen som batchen har lagra alle sine resultatfiler i
     */
    void informerOmSuksess(Path arbeidskatalog);


    /**
     * Notifiserer brukaren om at ein ukjent feil har hindra batchen frå å køyre ferdig og
     * at det er logga meir detaljert informasjon om feilen i loggfila til batchen.
     */
    void informerOmUkjentFeil();


    void informerOmOppryddingStartet();

    void informerOmKorrupteGrunnlagsdata(UgyldigUttrekkException e);

    void tidsseriegenereringFullfoert(Map<String, Integer> meldingar, String modusnavn);

    void informerOmGrunnlagsdataValidering();

    void informerOmMetadataOppretting();

    /**
     * Informerer bruker om at opprydding i kataloger feilet.
     */
    void informerOmFeiletOpprydding();

    void informerOmTimeout();
}
