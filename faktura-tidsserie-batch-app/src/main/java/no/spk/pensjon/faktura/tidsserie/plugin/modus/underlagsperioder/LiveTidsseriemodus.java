package no.spk.pensjon.faktura.tidsserie.plugin.modus.underlagsperioder;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.core.BehandleMedlemCommand;
import no.spk.pensjon.faktura.tidsserie.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.core.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.ServiceLocator;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.PrognoseRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link LiveTidsseriemodus} setter opp batchen til å generere ein live-tidsserie på periodenivå, formatert i
 * henhold til kontrakta for innmating til Qlikview og EDW/DVH.
 * <br>
 * Inntil vidare brukar live-tidsserien avregningsreglane sidan prognosereglane ikkje inkluderer støtte for beregning
 * av  YSK/GRU-faktureringsandel.
 * <br>
 * Ansvaret ofr generering av verdiane som endar opp i CSV-formatet for DVH-innmatinga, blir handtert av
 * {@link Datavarehusformat}.
 *
 * @author Tarjei Skorgenes
 * @see Datavarehusformat
 */
public class LiveTidsseriemodus implements Tidsseriemodus {
    private final CSVFormat outputFormat = new Datavarehusformat();

    private final Regelsett reglar = new PrognoseRegelsett();

    private final Tidsserienummer nummer = Tidsserienummer.genererForDato(now());

    @Override
    public void registerServices(ServiceRegistry serviceRegistry) {
        final ServiceLocator services = new ServiceLocator(serviceRegistry);
        final StorageBackend storage = services.firstMandatory(StorageBackend.class);
        serviceRegistry.registerService(AgentInitializer.class, kolonneskriver(storage));

        final Path tidsserieKatalog = services.firstMandatory(Path.class, Katalog.UT.egenskap());
        serviceRegistry.registerService(TidsserieLivssyklus.class, new LiveTidsserieAvslutter(tidsserieKatalog));
    }

    /**
     * Kolonnenavna CSV-formatet for live-tidsserien benyttar seg av.
     *
     * @return ein straum med alle kolonnenavna for live-tidsserien
     * @see Datavarehusformat#kolonnenavn()
     */
    @Override
    public Stream<String> kolonnenavn() {
        return outputFormat.kolonnenavn();
    }

    /**
     * Regelsettet som live-tidsserien skal benytte.
     *
     * @return regelsettet som skal benyttast ved oppbygging av live-tidsserien
     * @see AvregningsRegelsett
     */
    @Override
    public Regelsett regelsett() {
        return reglar;
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

    /**
     * Genererer ein ny publikator som genererer og lagrar ein observasjon pr underlagsperiode pr
     * observasjonsunderlag.
     * <br>
     *
     * @param facade      fasada som blir brukt for å generere tidsserien
     * @param serienummer serienummer som alle eventar som blir sendt vidare til <code>backend</code> for persistering
     *                    skal tilhøyre
     * @param publikator  backend-systemet som observasjonane av kvar periode blir lagra via
     * @return ein by publikator som serialiserer og lagrar alle underlagsperioder for kvart observasjonsunderlag i
     * tidsserien som blir generert av <code>facade</code>
     * @see Datavarehusformat#serialiser(Underlag, Underlagsperiode)
     * @see StorageBackend#lagre(Consumer)
     */
    @Override
    public Observasjonspublikator createPublikator(final TidsserieFacade facade, long serienummer, final StorageBackend publikator) {
        return nyPublikator(
                this::serialiserPeriode,
                line -> lagre(publikator, line, serienummer)
        );
    }

    /**
     * Genererer ein ny publikator som ved hjelp av <code>mapper </code> mappar alle observasjonsunderlagas perioder om
     * til ei form som deretter blir lagra via <code>lagring</code>.
     * <br>
     * Kvart observasjonsunderlag blir annotert med {@link no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer}
     * basert på dagens dato slik at mapperen unikt kan indikere at alle periodene tilhøyrer ein og samme tidsserie.
     *
     * @param mapper  serialiserer observasjonsunderlagas underlagsperioder til formatet <code>lagring</code> skal lagre på
     * @param lagring tar den serialiserte versjonen av underlagsperiodene og lagrar dei
     * @param <T>     datatypen underlagsperiodene blir serialisert til
     * @return ein ny observasjonspublikator som kan brukast til å serialisere og lagre innholdet frå ein tidsserie
     */
    <T> Observasjonspublikator nyPublikator(
            final Function<Underlag, Stream<T>> mapper, final Consumer<T> lagring) {
        return s -> s
                .peek(this::annoterMedTidsserienummer)
                .flatMap(mapper)
                .forEach(lagring);
    }

    private Underlag annoterMedTidsserienummer(Underlag u) {
        return u.annoter(Tidsserienummer.class, nummer);
    }

    private Stream<String> serialiserPeriode(final Underlag observasjonsunderlag) {
        return observasjonsunderlag
                .stream()
                .map(underlagsperiode -> outputFormat.serialiser(
                        observasjonsunderlag,
                        underlagsperiode
                ))
                .map(kolonneVerdiar -> kolonneVerdiar.map(Object::toString).collect(joining(";")));
    }

    private void lagre(final StorageBackend publikator, final String line, final long serienummer) {
        publikator.lagre(event -> event.serienummer(serienummer).buffer.append(line).append("\n"));
    }

    private AgentInitializer kolonneskriver(StorageBackend storage) {
        return serienummer -> storage.lagre(event -> event.serienummer(serienummer)
                .buffer
                .append(kolonnenavn().collect(joining(";")))
                .append('\n')
        );
    }
}
