package no.spk.pensjon.faktura.tidsserie.batch.core;

import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.medlem.Medlemsbehandler;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.felles.tidsperiode.underlag.reglar.Regelsett;
import no.spk.felles.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link Tidsseriemodus} er ansvarlig for oppretting og koordinering av {@link Observasjonspublikator} og
 * {@link CSVFormat} som tar seg av formatering og publisering av målingar basert på tidsserien.
 * <br>
 * Ettersom hensikta med målingane kan variere mellom forskjellige tidsseriar er modusen og ansvarlig
 * for kva {@link Regelsett} som skal benyttast ved oppbygging av tidsserien.
 *
 * @author Tarjei Skorgenes
 * @see CSVFormat
 * @see Observasjonspublikator
 * @see Regelsett
 */
public interface Tidsseriemodus extends Medlemsbehandler {

    /**
     * Metoden kalles før generering av tidsserie, slik at modusen kan legge til tjenesteregisteret med tjenster
     * den skal benytte senere.
     * <br>
     * Tjenester i tjenesteregisteret skal ikke brukes, da denne metoden blir kalt før tjenestene er initisialisert,
     * men etter at de er registrert.
     * Bruk av tjenestene bør tidligs skje først i {@link #lagTidsserie(ServiceRegistry)}  }
     *
     * @param serviceRegistry tjenesteregistertet som blir benyttet
     */
    void registerServices(ServiceRegistry serviceRegistry);
    /**
     * Returnerer ein straum med kolonnenavna som modusen vil generere verdiar for.
     *
     * @return output-formatet til tidsserien
     */
    Stream<String> kolonnenavn();

    /**
     * Beregningsreglane som tidsserien skal anvende seg av.
     *
     * @return gjeldande beregningsreglar for tidsserien
     */
    Regelsett regelsett();

    /**
     * Genererer ein ny tidsserie.
     * <br>
     * @return alle meldingar som har blitt generert i løpet av tidsseriegenereringa, gruppert på melding med antall
     * gangar meldinga var generert som verdi
     * @param registry tjenesteregister som benyttes for kjøringen
     */
    Map<String, Integer> lagTidsserie(ServiceRegistry registry);

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    default Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder) {
        return Stream.concat(
                perioder.loennsdata(),
                regelsett().reglar()
        );
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    default boolean behandleMedlem(Medlemsdata medlemsdata) {
        return true;
    }

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
