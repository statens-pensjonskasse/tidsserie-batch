package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;

/**
 * {@link no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory} tilbyr klientane enkelt oppretting av
 * dei sentrale datatypene og fasadene som trengst ved generering av nye tidsseriar.
 * <br>
 * Det er prim�rt {@link no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade} og
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata} som kan opprettast via denne fabrikken.
 * <br>
 * I tillegg er fabrikken ansvarlig for � koble inn/gi tilgang til avtale- og l�nnsdata som tidsserien kan benytte seg av.
 * <br>
 * Objekttypene fabrikken returnerer kan ikkje forventast � vere tr�d-sikre, objekta generert av fabrikken b�r derfor
 * ikkje delast p� tvers av tr�dar i samme JVM.
 *
 * @author Tarjei Skorgenes
 */
public interface TidsserieFactory {
    /**
     * Hentar ut alle tidsperiodiserte l�nnsdata som ikkje er medlemsspesifikke.
     *
     * @return alle l�nnsdata
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperioder
     */
    Stream<Tidsperiode<?>> loennsdata();

    /**
     * Opprettar ei ny tidsserie-fasade og pre-populerer den med avtaledata.
     * <br>
     * Fasada blir og satt opp til � bruke den angitte feilhandteringsstrategien for � handtere alle fatale feil p�
     * medlems- eller stillingsforholdniv�.
     *
     * @param feilhandtering feilhandteringsstrategi for feil som oppst�r ved oppbygging av medlems- eller stillingsforholdunderlag
     * @return ei ny tidsserie-fasade prepopulert med avtaledata og feilhandteringsstrategi
     * @see no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade#overstyr(no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository)
     * @see no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade#overstyr(no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering)
     */
    TidsserieFacade create(Feilhandtering feilhandtering);

    /**
     * Opprettar eit nytt sett med {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata} for eit medlem
     * basert p� CSV-data generert av faktura-tidsserie-batch.
     * <br>
     * Medlemsdatane blir populert med oversettarar som konverterer <code>data</code> (gitt at det er data som er generert av
     * faktura-grunnlagsdata-batch) til domeneobjekt for alle p�krevde/st�tta medlemsdata-typer.
     *
     * @param foedselsnummer f�dselsnummeret som unikt identifiserer medlemmet
     * @param data           medlemsdata p� CSV-format
     * @return eit nytt sett med medlemsdata, preopulert med data og oversettarar som st�ttar konvertering av desse til domeneobjekt
     */
    Medlemsdata create(final String foedselsnummer, final List<List<String>> data);
}
