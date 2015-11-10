package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;

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
     * @param serienummer serienummer som alle eventar som blir sendt vidare til <code>backend</code> for persistering
     * skal tilhøyre
     * @param backend backendtenesta for lagring av resultata publikatoren genererer
     * @return observasjonspublikatoren som skal benyttast av tidsseriegenereringa.
     */
    Observasjonspublikator create(final TidsserieFacade tidsserie, long serienummer, final StorageBackend backend);

    /**
     * Beregningsreglane som tidsserien skal anvende seg av.
     *
     * @return gjeldande beregningsreglar for tidsserien
     */
    Regelsett regelsett();

    /**
     * Oppretter eit nytt repository som les alt av grunnlagsdata frå ein bestemt katalog.
     *
     * @param directory katalogen som inneheld filene grunnlagsdata skal hentast frå
     * @return eit nytt repository som les grunnlagsdata frå den angitte katalogen
     * @since 1.2.0
     */
    default GrunnlagsdataRepository repository(Path directory) {
        return new CSVInput(directory);
    }

    /**
     * Globale referansedata som inneheld tidsperioder som hverken er avtale eller medlemsavhengige.
     * <br>
     * Dei viktigaste referanseperiodene er regelperiodene og lønnstrinna og grunnbeløpet som er tilgjengelig via
     * <code>perioder</code>. I tillegg kan modusen sende inn eventuelle andre globale perioder som alle
     * underlaga skal ta hensyn til.
     *
     * @param perioder factory for alle globale tidsperioder
     * @return alle medlems og avtale-uavhengige tidsperioder som skal leggast til på alle underlag
     * @since 1.2.0
     */
    default Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder) {
        return Stream.concat(
                perioder.loennsdata(),
                regelsett().reglar()
        );
    }

    /**
     * Benyttes for å tilpasse storage for modus-implmentasjonen. Kalles før jobbene for tidsserien startes.
     * <br> Default implementasjon er noop.
     *
     * @param storage som trenger tilpasset initisalisering for modusen.
     * @since 1.2.0
     */
    default void initStorage(StorageBackend storage) {
    }

    /**
     * Kalles én gang for hver partisjon i gridet, og angir hvilket serienummer partisjonen er tildelt.
     * Default implmentasjon lagrer {@link #kolonnenavn()} til {@link StorageBackend} for angitt serienummer.
     * @param serienummer nummer tildelt partisjonen i gridet
     * @param storage publisher for lagring av data
     */
    default void partitionInitialized(long serienummer, StorageBackend storage) {
        storage.lagre(event -> event.serienummer(serienummer)
                        .buffer
                        .append(kolonnenavn().collect(joining(";")))
                        .append('\n')
        );
    }

    /**
     * Kalles når tidsserien er ferdig generert, og angir oppsummering av resultatet.
     * <br> Default implementasjon er noop.
     * @param tidsserieResulat oppsummering av tidsseriekjoeringen
     */
    default void completed(TidsserieResulat tidsserieResulat) {
    }

    /**
     * Angir om et sett med medlemsdata er relevant å behandle for tidsseriemodusen. Tidsserien genereres gjerne utifra
     * et underlagsdatasett som er mer omfattende enn det man har behov for å behandle. Ved å overstyre denne metoden
     * kan man styre hvilke medlemsdata som ender opp i underlaget.
     * @param medlemsdata som kanskje skal behandles av modusen
     * @return true dersom angitt medlemsdata skal behandles, false ellers
     */
    default boolean behandleMedlem(Medlemsdata medlemsdata) {
        return true;
    }
}
