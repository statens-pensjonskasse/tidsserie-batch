package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.nio.file.Path;
import java.util.Map;

import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

/**
 * TODO: Kva og korleis �nskjer vi � vise status for batchk�yringa n�r vi k�yrer den for v�r egen bruk?
 */
public interface View {

    void startarBackend() ;

    void startarOpplasting() ;

    void opplastingFullfoert() ;

    void startarTidsseriegenerering(FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) ;

    /**
     * Viser informasjon om kva kommandolinjeargument batchen st�ttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det st�ttar.
     *
     * @param e hjelp-foresp�rslen som inneheld informasjon om tilgjengelige argument
     */
    void visHjelp(UsageRequestedException e);

    /**
     * Notifiserer brukaren om at eit av kommandolinjeargumenta som er angitt, er ugyldig med
     * informasjon om kva som er feil.
     *
     * @param e valideringsfeilen som inneheld informasjon om kva som er feil med argumentet
     */
    void informerOmUgyldigKommandolinjeArgument(InvalidParameterException e);

    void informerOmOppstart(ProgramArguments arguments) ;


    /**
     * Informerer brukaren om at batchen har k�yrt ferdig og at k�yringa gjekk f�re seg utan nokon feil.
     *
     * @param arbeidskatalog den nye arbeidskatalogen som batchen har lagra alle sine resultatfiler i
     */
    void informerOmSuksess(Path arbeidskatalog);


    /**
     * Notifiserer brukaren om at ein ukjent feil har hindra batchen fr� � k�yre ferdig og
     * at det er logga meir detaljert informasjon om feilen i loggfila til batchen.
     */
    void informerOmUkjentFeil();


    void informerOmOppryddingStartet();

    void informerOmKorrupteGrunnlagsdata(GrunnlagsdataException e);

    void tidsseriegenereringFullfoert(Map<String, Integer> meldingar);

    void informerOmGrunnlagsdataValidering();

    void informerOmMetadataOppretting();

    /**
     * Informerer bruker om at opprydding i kataloger feilet.
     */
    void informerOmFeiletOpprydding();

    void informerOmTimeout();
}
