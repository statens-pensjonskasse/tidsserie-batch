package no.spk.pensjon.faktura.tidsserie.batch.it;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ServiceLoader;

import no.spk.faktura.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.ConsoleView;
import no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataDirectoryValidator;
import no.spk.pensjon.faktura.tidsserie.batch.main.View;
import no.spk.pensjon.faktura.tidsserie.batch.core.BatchIdConstants;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.Constants;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java8.No;
import org.junit.rules.TemporaryFolder;

/**
 * Cucumber-definisjonar som støttar egenskapar som ønskjer å verifisere
 * CSV-outputen som ein modus i faktura-tidsserie-batch genererer.
 * <br>
 * Definisjonane tilrettelegger for at at ein setter opp navngitte CSV-filer
 * som inneheld grunnlagsdata som ein gitt modus i batchen skal behandle.
 * <br>
 * CSV-filene med tidsserien som batchen genererer, kan i etterkant av
 * køyringa verifiserast mot forventa resultat i tabellform frå egenskapen.
 * <br>
 * Definisjonane støttar og ignorering av navngitte kolonner som endrar
 * tilstand frå køyring til køyring og som dermed er umulige/krevande å
 * skulle verifisere eksakt match for.
 *
 * @author Tarjei Skorgenes
 */
public class EndeTilEndeModusDefinisjon implements No {
    private final MyTemporaryFolder temp = new MyTemporaryFolder();

    private final ModusRule modusar = new ModusRule();

    private final CSVFiler lagring = new CSVFiler();

    private Observasjonsperiode periode;

    private Modus modus;

    private File grunnlagsdata;

    private File utKatalog;

    private InMemoryBatchRunner batch;

    private Tidsseriefiler tidsseriefiler;

    public EndeTilEndeModusDefinisjon() {
        /**
         * Fyllord for å gjere egenskapen meir lesbar når ein ønskjer å verifisere kva modusar som er tilgjengelig.
         */
        Gitt("^at brukaren ønskjer å generere ein tidsserie$", this::noop);

        Gitt("^følgjande innhold i ([^\\.]+\\.csv\\.gz):$", this::lagreLinjerTilFil);
        Gitt("^at modus er lik (.+)$", this::medModus);
        Gitt("^observasjonsperioda strekker seg frå og med (\\d+) til og med (\\d+)$", this::medObservasjonsperiode);
        Gitt("^følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:$", this::ignorerKolonner);

        Så("^skal CSV-fil(?:a|ene) som blir generert inneholde følgjande rader:$", this::genererOgVerifiserResultat);

        Så("^skal følgjande modusar vere tilgjengelige for bruk:$", this::verifiserKvaModusarSomErTilgjengelige);
    }

    private void verifiserKvaModusarSomErTilgjengelige(final DataTable modusar) {
        final List<String> actual = modusar.transpose().topCells();
        actual.remove("Navn");
        assertThat(
                Modus
                        .stream()
                        .map(Modus::kode)
                        .collect(toList())
        )
                .containsOnlyElementsOf(
                        actual
                )
        ;
    }

    private void genererOgVerifiserResultat(final DataTable expected) {
        genererTidsserie();
        verifiserOutput(expected);
    }

    private void verifiserOutput(final DataTable expected) {
        expected.diff(
                tidsseriefiler.konverterTidsserierTilTabell(lagring)
        );
    }

    private void ignorerKolonner(final DataTable kolonner) {
        tidsseriefiler.ignorerKolonner(kolonner.asList(String.class));
    }

    private void medObservasjonsperiode(final String fraOgMed, final String tilOgMed) {
        this.periode = new Observasjonsperiode(
                parse(fraOgMed).atStartOfYear(),
                parse(tilOgMed).atEndOfYear()
        );
    }

    private void medModus(final String modus) {
        this.modus = Modus.parse(modus)
                .orElseThrow(() -> new AssertionError("Ukjent modus: " + modus));
    }

    private void noop() {
    }

    private void lagreLinjerTilFil(final String filename, final DataTable lines) {
        lagring.lagreLinjerTilFil(
                lines,
                new File(grunnlagsdata, filename).toPath()
        );
    }

    private void genererTidsserie() {
        batch.run(
                requireNonNull(temp.getRoot()),
                requireNonNull(utKatalog),
                requireNonNull(periode, "observasjonsperioder er påkrevd, men har ikke blitt satt opp av egenskapen"),
                requireNonNull(modus, "modus er påkrevd, men har ikke blitt satt opp av egenskapen")
        );
    }

    @Before
    public void _before() throws Throwable {
        temp.before();

        // Vi må autodetektere kva modusar som er tilgjengelig på samme måte som main-klassa gjer det
        modusar.autodetect();

        this.utKatalog = temp.getRoot();

        this.grunnlagsdata = new BatchId(BatchIdConstants.GRUNNLAGSDATA_PREFIX, LocalDateTime.now()).tilArbeidskatalog(temp.getRoot().toPath()).toFile();
        assertThat(grunnlagsdata.mkdir()).isTrue();

        final ServiceRegistry registry = ServiceLoader
                .load(ServiceRegistry.class)
                .iterator()
                .next();
        registry.registerService(View.class, new ConsoleView());

        this.batch = new InMemoryBatchRunner(registry);

        this.tidsseriefiler = new Tidsseriefiler(utKatalog);

        // Vi tar ikkje bryet med å generere gyldige sjekksummer for grunnlagsdatane, overstyrer derfor valideringen
        registry.registerService(GrunnlagsdataDirectoryValidator.class, this::noop, Constants.SERVICE_RANKING + "=1000");
    }

    @After
    public void _after() {
        temp.after();
        modusar.after();
    }

    private Aarstall parse(final String fraOgMed) {
        return of(fraOgMed).map(Integer::valueOf).map(Aarstall::new).get();
    }

    private static class MyTemporaryFolder extends TemporaryFolder {
        @Override
        public void before() throws Throwable {
            super.before();
        }

        @Override
        public void after() {
            super.after();
        }
    }
}
