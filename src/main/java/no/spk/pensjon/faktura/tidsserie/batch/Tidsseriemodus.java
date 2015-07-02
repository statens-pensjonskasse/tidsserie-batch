package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;

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
public interface Tidsseriemodus {
    /**
     * Returnerer ein straum med kolonnenavna som modusen vil generere verdiar for.
     *
     * @return output-formatet til tidsserien
     */
    Stream<String> kolonnenavn();

    /**
     * Genererer ein ny observasjonspublikator for observasjonsunderlaga som <code>tidsserie</code> vil generere.
     * <br>
     * Lagring av sluttresultatet skal skje via <code>backend</code>.
     *
     * @param tidsserie tidsseriefasada som publikatoren skal anvendast av
     * @param backend   backendtenesta for lagring av resultata publikatoren genererer
     * @return observasjonspublikatoren som skal benyttast av tidsseriegenereringa.
     */
    Observasjonspublikator create(final TidsserieFacade tidsserie, final StorageBackend backend);

    /**
     * Beregningsreglane som tidsserien skal anvende seg av.
     *
     * @return gjeldande beregningsreglar for tidsserien
     */
    Regelsett regelsett();
}
