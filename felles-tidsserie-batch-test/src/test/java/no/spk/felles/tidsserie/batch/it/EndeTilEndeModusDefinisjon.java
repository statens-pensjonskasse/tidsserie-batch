package no.spk.felles.tidsserie.batch.it;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.core.BatchIdConstants;
import no.spk.felles.tidsserie.batch.main.ConsoleView;
import no.spk.felles.tidsserie.batch.main.View;
import no.spk.felles.tidsserie.batch.main.input.Modus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java8.No;

/**
 * Cucumber-definisjonar som støttar egenskapar som ønskjer å verifisere
 * CSV-outputen som ein modus i felles-tidsserie-batch genererer.
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
    private final Map<String, Function<ServiceRegistry, FellesTidsserieBatch>> runas = new HashMap<>();

    public final TemporaryFolder temp = new TemporaryFolder();

    private final Md5Checksums checksums = new Md5Checksums();

    private final CSVFiler lagring = new CSVFiler();

    private Observasjonsperiode periode;

    private Modus modus;

    private File grunnlagsdata;

    private File utKatalog;

    private Tidsseriefiler tidsseriefiler;

    private ServiceRegistry registry;

    private Optional<Function<ServiceRegistry, FellesTidsserieBatch>> batch = empty();

    public EndeTilEndeModusDefinisjon() {
        runas.put("out-of-process", registry -> new OutOfProcessBatchRunner());
        runas.put("in-process", InMemoryBatchRunner::new);

        /*
         * Fyllord for å gjere egenskapen meir lesbar når ein ønskjer å verifisere kva modusar som er tilgjengelig.
         */
        Gitt("^at brukaren ønskjer å generere ein tidsserie$", this::noop);

        Gitt("^følgjande innhold i ([^\\.]+\\.csv\\.gz):$", this::lagreLinjerTilFil);
        Gitt("^(?:at )?modus er lik (.+)$", this::medModus);
        Gitt("^observasjonsperioda strekker seg frå og med (\\d+) til og med (\\d+)$", this::medObservasjonsperiode);
        Gitt("^følgjande kolonner blir ignorert fordi dei endrar verdi frå køyring til køyring:$", this::ignorerKolonner);
        Gitt("^(?:at )?batchen blir køyrt (.+)$", this::kjoerBatchenVia);

        Så("^skal CSV-fil(?:a|ene) som blir generert inneholde følgjande rader:$", this::genererOgVerifiserResultat);

        Så("^skal følgjande modusar vere tilgjengelige for bruk:$", this::verifiserKvaModusarSomErTilgjengelige);
    }

    private void verifiserKvaModusarSomErTilgjengelige(final DataTable modusar) {
        final List<String> actual = new ArrayList<>(
                modusar.transpose().row(0)
        );
        actual.remove("Navn");
        assertThat(
                Modus
                        .stream()
                        .map(Modus::kode)
                        .toList()
        )
                .containsOnlyOnceElementsOf(
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

    private void kjoerBatchenVia(final String name) {
        assertThat(this.runas.keySet())
                .as("støtta køyretyper for batchen")
                .contains(name);
        this.batch = Optional.of(this.runas.get(name));
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
        checksums.generer(grunnlagsdata);
        batch().run(
                requireNonNull(temp.getRoot().getAbsoluteFile()),
                requireNonNull(utKatalog),
                requireNonNull(periode, "observasjonsperioder er påkrevd, men har ikke blitt satt opp av egenskapen"),
                requireNonNull(modus, "modus er påkrevd, men har ikke blitt satt opp av egenskapen")
        );
    }

    @Before
    public void _before() throws Throwable {
        temp.create();
        // Vi må autodetektere kva modusar som er tilgjengelig på samme måte som main-klassa gjer det
        Modus.autodetect();
        this.utKatalog = temp.newFolder();

        this.grunnlagsdata = new BatchId(BatchIdConstants.GRUNNLAGSDATA_PREFIX, LocalDateTime.now()).tilArbeidskatalog(temp.getRoot().toPath()).toFile();
        assertThat(grunnlagsdata.mkdir()).isTrue();

        registry = ServiceLoader
                .load(ServiceRegistry.class)
                .iterator()
                .next();
        registry.registerService(View.class, new ConsoleView());

        this.tidsseriefiler = new Tidsseriefiler(utKatalog);
    }

    @After
    public void _after() {
        temp.delete();
        Modus.reload(Stream.empty());
    }

    private FellesTidsserieBatch batch() {
        return batch.orElseThrow(
                        () -> new IllegalArgumentException(
                                "Du har gløymt å angit korleis batchen skal køyrast, " +
                                        "bruk \"batchen blir køyrt TYPE\" som syntax " +
                                        "(lovlige typer: " + runas.keySet() + ")"
                        )
                )
                .apply(registry);
    }

    private Aarstall parse(final String fraOgMed) {
        return of(fraOgMed).map(Integer::valueOf).map(Aarstall::new).get();
    }
}
