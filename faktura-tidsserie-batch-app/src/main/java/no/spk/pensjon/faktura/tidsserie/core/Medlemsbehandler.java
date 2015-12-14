package no.spk.pensjon.faktura.tidsserie.core;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;

/**
 *
 * Interface for å styre hvilke medlemmer {@link BehandleMedlemCommand} som skal behandles av
 * {@link BehandleMedlemCommand}, hvilke referansedata som skal benyttes og hvordan medlemmer skal
 * publiseres.
 * @author Snorre E. Brekke - Computas
 * @since 2.0.0
 */
public interface Medlemsbehandler {

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
     * @since 2.0.0
     */
    Observasjonspublikator createPublikator(final TidsserieFacade tidsserie, long serienummer, final StorageBackend backend);

    /**
     * Globale referansedata som inneheld tidsperioder som hverken er avtale eller medlemsavhengige.
     * <br>
     * Dei viktigaste referanseperiodene er regelperiodene og lønnstrinna og grunnbeløpet som er tilgjengelig via
     * <code>perioder</code>. I tillegg kan modusen sende inn eventuelle andre globale perioder som alle
     * underlaga skal ta hensyn til.
     *
     * @param perioder factory for alle globale tidsperioder
     * @return alle medlems og avtale-uavhengige tidsperioder som skal leggast til på alle underlag
     */
    Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder);

    /**
     * Angir om et sett med medlemsdata er relevant for publisering. Tidsserien genereres gjerne utifra
     * et underlagsdatasett som er mer omfattende enn det man har behov for å behandle. Ved å overstyre denne metoden
     * kan man styre hvilke medlemmer som ender opp i tidsserien.
     * @param medlemsdata som kanskje skal behandles av modusen
     * @return true dersom angitt medlemsdata skal behandles, false ellers
     */
    boolean behandleMedlem(Medlemsdata medlemsdata);
}
