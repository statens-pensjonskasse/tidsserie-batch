package no.spk.pensjon.faktura.tidsserie.core;

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
     * Metoden kalles før generering av tidsserie, slik at modusen kan utvide tjenesteregisteret med tjenster
     * den skal benytte senere.
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
     * Konstruerer ein ny kommando som koordinerer mot dei angitte tenestene når tidsseriar pr medlem blir generert
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
