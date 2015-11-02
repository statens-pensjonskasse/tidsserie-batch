package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;

/**
 * {@link no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory} tilbyr klientane enkelt oppretting av
 * dei sentrale datatypene og fasadene som trengst ved generering av nye tidsseriar.
 * <br>
 * Det er primært {@link no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade} og
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata} som kan opprettast via denne fabrikken.
 * <br>
 * I tillegg er fabrikken ansvarlig for å koble inn/gi tilgang til avtale- og lønnsdata som tidsserien kan benytte seg av.
 * <br>
 * Objekttypene fabrikken returnerer kan ikkje forventast å vere tråd-sikre, objekta generert av fabrikken bør derfor
 * ikkje delast på tvers av trådar i samme JVM.
 *
 * @author Tarjei Skorgenes
 */
public interface TidsserieFactory extends TidsperiodeFactory {
    /**
     * Opprettar ei ny tidsserie-fasade og pre-populerer den med avtaledata.
     * <br>
     * Fasada blir og satt opp til å bruke den angitte feilhandteringsstrategien for å handtere alle fatale feil på
     * medlems- eller stillingsforholdnivå.
     *
     * @param feilhandtering feilhandteringsstrategi for feil som oppstår ved oppbygging av medlems- eller stillingsforholdunderlag
     * @return ei ny tidsserie-fasade prepopulert med avtaledata og feilhandteringsstrategi
     * @see no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade#overstyr(no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository)
     * @see no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade#overstyr(no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering)
     */
    TidsserieFacade create(Feilhandtering feilhandtering);

    /**
     * Opprettar eit nytt sett med {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata} for eit medlem
     * basert på CSV-data generert av faktura-tidsserie-batch.
     * <br>
     * Medlemsdatane blir populert med oversettarar som konverterer <code>data</code> (gitt at det er data som er generert av
     * faktura-grunnlagsdata-batch) til domeneobjekt for alle påkrevde/støtta medlemsdata-typer.
     *
     * @param data medlemsdata på CSV-format
     * @return eit nytt sett med medlemsdata, preopulert med data og oversettarar som støttar konvertering av desse til domeneobjekt
     */
    Medlemsdata create(final List<List<String>> data);
}
