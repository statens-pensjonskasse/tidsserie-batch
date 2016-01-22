package no.spk.pensjon.faktura.tidsserie.plugin.modus.prognoseobservasjonar;

import static java.util.stream.Collectors.joining;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.core.BehandleMedlemCommand;
import no.spk.pensjon.faktura.tidsserie.core.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.core.ServiceLocator;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aarsverk;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.PrognoseRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link Stillingsforholdprognosemodus} setter opp batchen til å generere månedlige observasjonar
 * på stillingsforholdnivå.
 * <br>
 * Observasjonane blir seinare (aka utanfor batchen) aggregert til avtalenivå og mata inn i MySQL for bruk av FAN
 * ved generering av ny eller oppdagerte prognoser.
 * <br>
 * Formatet forventast fasa ut i løpet av 2015 når live-tidsserien blir mata inn i Qlikview / DVH regelmessig.
 *
 * @author Tarjei Skorgenes
 */
public class Stillingsforholdprognosemodus implements Tidsseriemodus {
    private final ThreadLocal<NumberFormat> format = new ThreadLocal<>();

    @Override
    public void registerServices(final ServiceRegistry serviceRegistry) {
        final ServiceLocator services = new ServiceLocator(serviceRegistry);
        final StorageBackend storage = services.firstMandatory(StorageBackend.class);
        serviceRegistry.registerService(AgentInitializer.class, kolonneskriver(storage));
    }

    /**
     * Kolonnenavna for kolonnene som prognoseobservasjonane består.
     * <br>
     * Kolonnene er som følger:
     * <ol>
     * <li>avtaleId</li>
     * <li>stillingsforholdId</li>
     * <li>observasjonsdato</li>
     * <li>maskinelt_grunnlag</li>
     * <li>premiestatus (ustabil/ustøtta)</li>
     * <li>årsverk</li>
     * <li>personnummer (uimplementert)</li>
     * </ol>
     *
     * @return ein straum med dei 7 kolonnene som modusen genererer data for på aggregert nivå
     */
    @Override
    public Stream<String> kolonnenavn() {
        return Stream.of(
                "avtaleId",
                "stillingsforholdId",
                "observasjonsdato",
                "maskinelt_grunnlag",
                "premiestatus",
                "årsverk",
                "personnummer"
        );
    }

    /**
     * Genererer ein observasjonspublikator som aggregerer alle underlagsperioder pr observasjonsunderlag,
     * til stillingsforhold- og avtalenivå.
     * <br>
     * Dei aggregerte målingane blir serialisert til tekst og lagra via <code>lagring</code>.
     * <br>
     * Merk at sjølv om personnummer er med som ei kolonne i denne modusen så blir den ikkje
     * populert med gyldige data sidan informasjon om medlemmet ikkje er inkludert i aggregeringa av observasjonane.
     * <br>
     * Merk og at premiestatusen som blir lista ut ikkje er pålitelig i situasjonar der stillingsforholdet
     * har vore gjennom eit avtalebytte.
     *
     * @param tidsserie   tidsserien som publikatoren skal integrere mot
     * @param serienummer serienummer som alle eventar som blir sendt vidare til <code>backend</code> for persistering
     *                    skal tilhøyre
     * @param lagring     backend-systemet som skal ta i mot og lagre unna observasjonane som blir generert
     * @return ein ny observasjonspublikator for prognoseobservasjonar og stillingsforhold- og avtalenivå
     * @see TidsserieFacade#lagObservasjonsaggregatorPrStillingsforholdOgAvtale(Consumer)
     */
    @Override
    public Observasjonspublikator createPublikator(final TidsserieFacade tidsserie, long serienummer, StorageBackend lagring) {
        final Consumer<TidsserieObservasjon> konsument = o -> {
            lagring.lagre(event -> {
                event
                        .serienummer(serienummer)
                        .buffer
                        .append(o.avtale().id())
                        .append(';')
                        .append(o.stillingsforhold.id())
                        .append(';')
                        .append(o.observasjonsdato.dato())
                        .append(';')
                        .append(o.maskineltGrunnlag.verdi())
                        .append(';')
                        .append(o.premiestatus().map(Premiestatus::kode).orElse("UKJENT"))
                        .append(';')
                        .append(Optional.of(o.aarsverk())
                                .map(Aarsverk::tilProsent)
                                .map(Prosent::toDouble)
                                .map(aarsverkformat()::format)
                                .orElse("0.0")
                        )
                        .append(';')
                        .append("MISSING")
                        .append('\n');
            });
        };
        return tidsserie.lagObservasjonsaggregatorPrStillingsforholdOgAvtale(konsument);
    }

    /**
     * Returnerer regelsettet som skal benyttast ved generering av prognoseobservasjonar.
     *
     * @return eit nytt sett med reglar for prognoseformål
     * @see PrognoseRegelsett
     */
    @Override
    public Regelsett regelsett() {
        return new PrognoseRegelsett();
    }

    private NumberFormat aarsverkformat() {
        NumberFormat format = this.format.get();
        if (format == null) {
            format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(3);
            format.setMinimumFractionDigits(1);
            this.format.set(format);
        }
        return format;
    }

    @Override
    public Map<String, Integer> lagTidsserie(ServiceRegistry registry) {
        final ServiceLocator services = new ServiceLocator(registry);
        final StorageBackend storage = services.firstMandatory(StorageBackend.class);
        final TidsserieFactory tidsserieFactory = services.firstMandatory(TidsserieFactory.class);
        final TidsserieBackendService tidsserieService = services.firstMandatory(TidsserieBackendService.class);

        final GenererTidsserieCommand command = new BehandleMedlemCommand(tidsserieFactory, storage, this);
        registry.registerService(GenererTidsserieCommand.class, command);
        return tidsserieService.lagTidsserie();
    }

    private AgentInitializer kolonneskriver(StorageBackend storage) {
        return serienummer -> storage.lagre(event -> event.serienummer(serienummer)
                .buffer
                .append(kolonnenavn().collect(joining(";")))
                .append('\n')
        );
    }
}