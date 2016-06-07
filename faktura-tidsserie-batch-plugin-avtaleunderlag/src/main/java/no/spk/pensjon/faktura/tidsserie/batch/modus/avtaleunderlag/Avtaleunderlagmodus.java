package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.stream.Collectors.toList;
import static no.spk.faktura.input.BatchId.fromString;
import static no.spk.pensjon.faktura.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PREFIX;
import static no.spk.pensjon.faktura.tjenesteregister.Constants.SERVICE_RANKING;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.batch.core.ServiceLocator;
import no.spk.pensjon.faktura.tidsserie.batch.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.batch.core.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;
import no.spk.pensjon.faktura.tjenesteregister.Constants;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Avtaleunderlagmodus lager underlagsperioder for avtaler.
 * <br>
 * Se <a href="http://wiki.spk.no/display/dok/Systemdokumentasjon+-+PU_FAK_BA_10+-+Modus+-+Avtaleunderlag">systemdokumentasjonen</a> for mer informasjon
 * om avtaleunderlagets oppbygging og hovedkunsomenter.
 *
 * @author Snorre E. Brekke - Computas
 * @see AvtaleunderlagFactory
 * @see Avtaleunderlagformat
 */
public class Avtaleunderlagmodus implements Tidsseriemodus {

    private final CSVFormat outputFormat = new Avtaleunderlagformat();
    private final Regelsett regler = new AvtaleunderlagRegelsett();

    private Optional<Underlagskriver> avtaleunderlagskriver = Optional.empty();

    /**
     * Modusen heiter {@code live_tidsserie}.
     */
    @Override
    public String navn() {
        return "avtaleunderlag";
    }

    @Override
    public void registerServices(final ServiceRegistry serviceRegistry) {
        final ServiceLocator services = new ServiceLocator(serviceRegistry);
        serviceRegistry.registerService(
                GrunnlagsdataRepository.class,
                repository(
                        services.firstMandatory(Path.class, Katalog.GRUNNLAGSDATA.egenskap())
                ),
                Constants.SERVICE_RANKING + "=1000"
        );

        final Path tidsserieKatalog = services.firstMandatory(Path.class, Katalog.UT.egenskap());
        serviceRegistry.registerService(TidsserieGenerertCallback.class,
                new AvtaleunderlagAvslutter(tidsserieKatalog),
                SERVICE_RANKING + "=1000"
        );
    }

    GrunnlagsdataRepository repository(final Path directory) {
        return new ReferansedataCSVInput(directory);
    }

    @Override
    public Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder) {
        return Stream.empty();
    }

    @Override
    public Stream<String> kolonnenavn() {
        return outputFormat.kolonnenavn();
    }

    @Override
    public Regelsett regelsett() {
        return regler;
    }

    @Override
    public Observasjonspublikator createPublikator(TidsserieFacade tidsserie, long serienummer, StorageBackend storage) {
        return o -> {};
    }

    @Override
    public Map<String, Integer> lagTidsserie(ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final Observasjonsperiode observasjonsperiode = locator.firstMandatory(Observasjonsperiode.class);
        final StorageBackend storage = locator.firstMandatory(StorageBackend.class);
        final Path grunnlagsdata = locator.firstMandatory(Path.class, Katalog.GRUNNLAGSDATA.egenskap());
        final TidsperiodeFactory tidsperieodeFactory = locator.firstMandatory(TidsperiodeFactory.class);

        final AvtaleunderlagFactory factory = new AvtaleunderlagFactory(tidsperieodeFactory, regelsett());
        final List<Underlag> underlag = factory.lagAvtaleunderlag(observasjonsperiode, uttrekksdato(grunnlagsdata)).collect(toList());
        lagreUnderlag(storage, underlag.stream());

        return resultat(underlag);
    }

    private Map<String, Integer> resultat(final List<Underlag> underlag) {
        Map<String, Integer> result = new HashMap<>();
        result.put("avtaler", antallAvtaler(underlag));
        return result;
    }

    private int antallAvtaler(List<Underlag> underlag) {
        return (int) underlag
                .stream()
                .map(u -> u.valgfriAnnotasjonFor(AvtaleId.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .count();
    }

    private Uttrekksdato uttrekksdato(final Path grunnlagsdata) {
        final LocalDate untrekksdato =
                fromString(GRUNNLAGSDATA_PREFIX, grunnlagsdata.getFileName().toString())
                        .asLocalDateTime()
                        .toLocalDate();
        return new Uttrekksdato(untrekksdato);
    }

    private void lagreUnderlag(StorageBackend storage, Stream<Underlag> underlag) {
        avtaleunderlagskriver
                .orElse(new Underlagskriver(storage, outputFormat))
                .lagreUnderlag(underlag);
    }

    void avtaleunderlagsskriver(Underlagskriver avtaleunderlagskriver) {
        this.avtaleunderlagskriver = Optional.of(avtaleunderlagskriver);
    }

    static class ReferansedataCSVInput extends CSVInput {
        private ReferansedataCSVInput(final Path directory) {
            super(directory);
        }

        @Override
        public Stream<List<String>> medlemsdata() {
            return Stream.empty();
        }
    }
}
