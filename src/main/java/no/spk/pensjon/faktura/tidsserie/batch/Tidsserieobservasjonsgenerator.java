package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link Tidsserieobservasjonsgenerator} er ein generator som basert på eit medlems stillingsforhold
 * genererer ein tidsserie beståande av 1 observasjon pr siste dag i kvar måned som ligg innanfor ei avgrensa
 * observasjonsperiode.
 * <p>
 * Generatoren er tilstandsfull og støttar ikkje prosessering der ein deler samme generatorinstans på tvers av
 * fleire trådar.
 *
 * @author Tarjei Skorgenes
 */
public class Tidsserieobservasjonsgenerator {
    private final List<Tidsperiode<?>> referanseperioder = new ArrayList<>();

    private final TidsserieFacade tidsserie = new TidsserieFacade();

    {
        tidsserie.overstyr(a -> Stream.empty());
    }

    public Tidsserieobservasjonsgenerator overstyr(final AvtaleinformasjonRepository repository) {
        tidsserie.overstyr(requireNonNull(repository));
        return this;
    }

    /**
     * Genererer ein ny tidsserie basert på medlemmet sine medlemsdata, relaterte avtaledata og referansedata.
     * <br>
     * Tidsserien strekker seg frå første til siste dag i den angitte observasjonsperioda og vil bli danna basert på
     * eit observasjonsunderlag generert pr siste dag i kvar måned innanfor denne perioda.
     * <br>
     * Kvart observasjonsunderlag blir sendt vidare til <code>publikator</code> for vidare behandling eller lagring.
     * <br>
     * Dersom publikatoren har behov for å gjere beregningar på periodene i observasjonsunderlaget må det skje
     * ved hjelp av beregningsreglane sendt inn via <code>reglar</code>.
     * <br>
     * Dersom det oppstår ein feil i forbindelse med prosesseringa av medlemmet eller eit av medlemmets
     * stillingsforhold, blir feilen sendt vidare til <code>feilhandtering</code> for vidare handtering.
     *
     * @param medlem              medlemsdata som det skal genererast ein tidsserie frå
     * @param observasjonsperiode perioda tidsserien maksimalt skal strekke seg over
     * @param publikator          mottakar av alle tidsserieobservasjonar som blir generert for medlemmet
     * @param feilhandtering      feilhandteringsstrategi som alle fatale feil underveis i tidsseriegenereringa vil bli
     *                            delegert til
     * @param reglar              regelsettet som tidsseriens beregna verdiar skal bli generert via
     */
    public void generer(final Medlemsdata medlem,
                        final Observasjonsperiode observasjonsperiode,
                        final Observasjonspublikator publikator,
                        final Feilhandtering feilhandtering,
                        final Regelsett reglar) {
        tidsserie.overstyr(feilhandtering);
        tidsserie.generer(
                medlem,
                observasjonsperiode,
                publikator,
                Stream.concat(
                        reglar.reglar(),
                        referanseperioder.stream()
                )
        );
    }

    public void registrer(final Stream<Tidsperiode<?>> referanseperioder) {
        referanseperioder.forEach(this.referanseperioder::add);
    }

    public Observasjonspublikator lagObservasjonsaggregatorPrStillingsforholdOgAvtale(
            final Consumer<TidsserieObservasjon> publikator) {
        return tidsserie.lagObservasjonsaggregatorPrStillingsforholdOgAvtale(publikator);
    }
}
