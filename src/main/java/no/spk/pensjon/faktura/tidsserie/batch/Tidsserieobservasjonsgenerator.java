package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsverkRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.DeltidsjustertLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.GruppelivsfaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.LoennstilleggRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MaskineltGrunnlagRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MedregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MinstegrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.OevreLoennsgrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelperiode;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.YrkesskadefaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link Tidsserieobservasjonsgenerator} er ein generator som basert p� eit medlems stillingsforhold
 * genererer ein tidsserie best�ande av 1 observasjon pr siste dag i kvar m�ned som ligg innanfor ei avgrensa
 * observasjonsperiode.
 * <p>
 * Generatoren er tilstandsfull og st�ttar ikkje prosessering der ein deler samme generatorinstans p� tvers av
 * fleire tr�dar.
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
     * Genererer eitt nytt datasett for bruk til � forklare vekst i pensjonsgivande �rsl�nn pr dags dato.
     * <p>
     * Tidsserien strekker seg fr� f�rste til siste dag i den angitte observasjonsperioda og vil inneholde ein
     * observasjon pr siste dag i kvar m�ned innanfor denne perioda.
     * <p>
     * Kvart observasjonsunderlag blir sendt vidare til <code>publikator</code> for vidare behandling eller lagring.
     * <p>
     * Dersom det oppst�r ein feil i forbindelse med prosesseringa av medlemmet eller eit av medlemmets
     * stillingsforhold, blir feilen sendt vidare til <code>feilhandtering</code> for vidare handtering.
     */
    public void observerForVekstmodell(final Medlemsdata medlem, final Observasjonsperiode observasjonsperiode,
                                       final Observasjonspublikator publikator,
                                       final Feilhandtering feilhandtering) {
        tidsserie.overstyr(feilhandtering);
        observer(medlem, observasjonsperiode, publikator, tidsserie);
    }

    private void observer(Medlemsdata medlem, Observasjonsperiode observasjonsperiode, Observasjonspublikator publikator, TidsserieFacade tidsserie) {
        tidsserie.generer(
                medlem,
                observasjonsperiode,
                publikator,
                Stream.concat(
                        regelset(),
                        referanseperioder.stream()
                )
        );
    }

    /**
     * Genererer regelsettet som blir benytta n�r ein skal gjere beregningar i forbindelse med kvar
     * tidsserieobservasjon.
     * <p>
     * Regelsettet blir generert p� ein slik m�te at ein skal kunne bruke gjeldande reglar i dag (2015) bakover
     * i til og med �r 2000.
     * <p>
     * �r 2000 er tilfeldig valgt basert p� ein antagelse om at prognosene som tidsserien blir brukt p�, ikkje
     * kjem til � ha behov for tidsseriar lenger enn dette.
     */
    private Stream<Tidsperiode<?>> regelset() {
        return Stream.<Tidsperiode<?>>of(
                new Regelperiode<>(dato("2000.01.01"), empty(), new MaskineltGrunnlagRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new AarsfaktorRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new DeltidsjustertLoennRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new AntallDagarRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new AarsLengdeRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new LoennstilleggRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new OevreLoennsgrenseRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new MedregningsRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new MinstegrenseRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new AarsverkRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new GruppelivsfaktureringRegel()),
                new Regelperiode<>(dato("2000.01.01"), empty(), new YrkesskadefaktureringRegel())
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
