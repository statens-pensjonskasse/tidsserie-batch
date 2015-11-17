package no.spk.pensjon.faktura.tidsserie.core;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;

/**
 * {@link Tidsseriemodus} er ansvarlig for oppretting og koordinering av {@link Observasjonspublikator} og
 * {@link CSVFormat} som tar seg av formatering og publisering av m�lingar basert p� tidsserien.
 * <br>
 * Ettersom hensikta med m�lingane kan variere mellom forskjellige tidsseriar er modusen og ansvarlig
 * for kva {@link Regelsett} som skal benyttast ved oppbygging av tidsserien.
 *
 * @author Tarjei Skorgenes
 * @see CSVFormat
 * @see Observasjonspublikator
 * @see Regelsett
 */
public interface Tidsseriemodus extends Medlemsbehandler {
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
     * Oppretter eit nytt repository som les alt av grunnlagsdata fr� ein bestemt katalog.
     *
     * @param directory katalogen som inneheld filene grunnlagsdata skal hentast fr�
     * @return eit nytt repository som les grunnlagsdata fr� den angitte katalogen
     * @since 1.2.0
     */
    default GrunnlagsdataRepository repository(Path directory) {
        return new CSVInput(directory);
    }

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
     * Benyttes for � tilpasse storage for modus-implmentasjonen. Kalles f�r jobbene for tidsserien startes.
     * <br> Default implementasjon er noop.
     *
     * @param storage som trenger tilpasset initisalisering for modusen.
     * @since 1.2.0
     */
    default void initStorage(StorageBackend storage) {
    }

    /**
     * Kalles �n gang for hver partisjon i gridet, og angir hvilket serienummer partisjonen er tildelt.
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
     * Kalles n�r tidsserien er ferdig generert, og angir oppsummering av resultatet.
     * <br> Default implementasjon er noop.
     * @param tidsserieResulat oppsummering av tidsseriekjoeringen
     */
    default void completed(TidsserieResulat tidsserieResulat) {
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    default boolean behandleMedlem(Medlemsdata medlemsdata) {
        return true;
    }

    /**
     * Konstruerer ein ny kommando som koordinerer mot dei angitte tenestene n�r tidsseriar pr medlem blir generert
     * av {@link GenererTidsserieCommand#generer(List, Observasjonsperiode, Feilhandtering, long)}.
     *
     * @param grunnlagsdata tenesta som gir tilgang til grunnlagsdata som ikkje er medlemsspesifikke
     * @param lagring tenesta som lagrar observasjonane generert av
     * {@link Tidsseriemodus#createPublikator(TidsserieFacade, long, StorageBackend)}
     * @return GenererTidsserieCommand
     * @see GenererTidsserieCommand
     * @since 2.0.0
     */
    default GenererTidsserieCommand createTidsserieCommand(final TidsserieFactory grunnlagsdata, final StorageBackend lagring){
        return new BehandleMedlemCommand(grunnlagsdata, lagring, this);
    }
}
