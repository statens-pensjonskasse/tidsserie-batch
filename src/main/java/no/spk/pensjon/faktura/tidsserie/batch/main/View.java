package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.nio.file.Path;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.main.GrunnlagsdataException;
import no.spk.pensjon.faktura.tidsserie.storage.main.Oppryddingsstatus;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.UsageRequestedException;

/**
 * TODO: Kva og korleis ønskjer vi å vise status for batchkøyringa når vi køyrer den for vår egen bruk?
 */
public interface View {

    void startarBackend() ;

    void startarOpplasting() ;

    void opplastingFullfoert() ;

    void startarTidsseriegenerering(FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) ;

    /**
     * Viser informasjon om kva kommandolinjeargument batchen støttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det støttar.
     *
     * @param e hjelp-forespørslen som inneheld informasjon om tilgjengelige argument
     */
    void visHjelp(UsageRequestedException e);

    /**
     * Notifiserer brukaren om at eit av kommandolinjeargumenta som er angitt, er ugyldig med
     * informasjon om kva som er feil.
     *
     * @param e valideringsfeilen som inneheld informasjon om kva som er feil med argumentet
     */
    void informerOmUgyldigKommandolinjeArgument(ProgramArgumentsFactory.InvalidParameterException e);

    void informerOmOppstart(ProgramArguments arguments) ;


    /**
     * Notifiserer brukaren om at oppryddingssteget til batchen ikkje har klart å slette
     * alle arbeidskatalogar som er eldre enn det antall dagar brukaren har angitt som nedre øvre
     * grenseverdi for køyringstidspunkt.
     *
     * @param status status som inneheld informasjon om alle arbeidskatalogane som batchen ikkje klarte å slette
     */
    void informerOmUslettbareArbeidskatalogar(Oppryddingsstatus status);

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

    void informerOmKorrupteGrunnlagsdata(GrunnlagsdataException e);

    void tidsseriegenereringFullfoert(Map<String, Integer> meldingar);
}
