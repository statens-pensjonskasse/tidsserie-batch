package no.spk.premie.tidsserie.batch.main.input;

import static java.time.LocalDate.now;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.faktura.input.ArgumentSummary;
import no.spk.faktura.input.Arguments;
import no.spk.faktura.input.DurationUtil;
import no.spk.faktura.input.DurationValidator;
import no.spk.faktura.input.IntegerValidator;
import no.spk.faktura.input.LocalTimeConverter;
import no.spk.faktura.input.LocalTimeValidator;
import no.spk.faktura.input.PathStringValidator;
import no.spk.faktura.input.PrintbareProgramargumenter;
import no.spk.faktura.input.ReadablePathValidator;
import no.spk.faktura.input.WritableDirectoryValidator;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.premie.tidsserie.batch.core.Tidsseriemodus;
import no.spk.premie.tidsserie.batch.core.UttrekksId;
import no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;
import no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.premie.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ProgramArguments implements Arguments, TidsserieBatchArgumenter, PrintbareProgramargumenter {

    @Spec
    CommandSpec spec;

    Path innkatalog;
    UttrekksId uttrekk;
    Path utkatalog;
    Path logkatalog;
    int fraAar = new StandardBatchperiode(now()).fraAar();
    int tilAar = new StandardBatchperiode(now()).tilAar();
    AntallProsessorar nodes = AntallProsessorar.standardAntallProsessorar();
    int antallNoderForPrinting;
    Modus modus;
    String kjoeretid = "0400";
    LocalTime sluttidspunkt = LocalTime.parse("23:59");

    @Option(names = {"-hjelp", "-?", "-h", "-help"},
            usageHelp = true,
            description = "Viser denne brukerveiledningen."
    )
    boolean hjelp;

    @Option(names = {"-b"},
            description = "Tekstlig beskrivelse av hensikten med kjøringen.",
            required = true
    )
    String beskrivelse;

    @Option(names = {"-i"},
            description = "En katalog som inneholder 1 eller flere uttrekkskataloger som batchen kan benytte for innlesing av grunnlagsdata.",
            required = true
    )
    public void settInnkatalog(final String value) {
        new PathStringValidator().validate("i", value, spec);
        final Path path = Paths.get(value);
        new ReadablePathValidator().validate("i", path, spec);
        innkatalog = path;
    }

    @Option(names = {"-id"},
            description = "En tekstlig kode som identifiserer hvilken uttrekkskatalog i inn-katalogen som kjøringen skal benytte. Dersom -id mangler benyttes det nyeste uttrekket."
    )
    public void settUttrekk(final String value) {
        new BatchIdValidator().validate("id", value, spec);
        uttrekk = new UttrekksIdConverter().convert(value);
    }

    @Option(names = {"-o"},
            description = "Utkatalogen der resultatet av kjøringen blir lagret.",
            required = true
    )
    public void settUtkatalog(final String value) {
        new PathStringValidator().validate("o", value, spec);
        final Path path = Paths.get(value);
        new WritableDirectoryValidator().validate("o", path, spec);
        utkatalog = path;
    }

    @Option(names = {"-log"},
            description = "Batchen vil lage en ny katalog i -log katalogen hvor batch.log og metadata for kjøringen vil bli lagret.",
            required = true
    )
    public void settLogkatalog(final String value) {
        new PathStringValidator().validate("log", value, spec);
        final Path path = Paths.get(value);
        new WritableDirectoryValidator().validate("log", path, spec);
        logkatalog = path;
    }

    @Option(names = {"-fraAar"},
            description = "Tidsserien lages fra og med 01.01 i angitt år."
    )
    public void settFraAar(final String value) {
        new IntegerValidator().validate("fraAar", value, spec);
        final int heltall = Integer.parseInt(value);
        new YearValidator().validate("fraAar", heltall, spec);
        fraAar = heltall;
    }

    @Option(names = {"-tilAar"},
            description = "Tidsserien lages til og med 31.12 i angitt år."
    )
    public void settTilAar(final String value) {
        new IntegerValidator().validate("tilAar", value, spec);
        final int heltall = Integer.parseInt(value);
        new YearValidator().validate("tilAar", heltall, spec);
        tilAar = heltall;
    }

    @Option(names = {"-n"},
            description = "Antall noder som skal brukes for å utgjøre grid for tidsserie-prossesering. Default er lik antall prosessorer på serveren minus 1."
    )
    public void settNoder(final String value) {
        new AntallProsessorarValidator().validate("n", value, spec);
        nodes = new AntallProsessorarConverter().convert(value);
        antallNoderForPrinting = Integer.parseInt(value);
    }

    @Option(names = {"-m"},
            description = "Modusen batchen skal bruke for oppbygging av og lagring av tidsserien.",
            required = true
    )
    public void settModus(final String value) {
        new ModusValidator().validate("m", value, spec);
        modus = new ModusConverter().convert(value);
    }

    @Option(names = "-kjoeretid",
            description = "Maks kjøretid på formatet HHmm."
    )
    public void settKjoeretid(final String value) {
        new DurationValidator().validate("kjoeretid", value, spec);
        kjoeretid = value;
    }

    @Option(names = {"-sluttid"},
            description = "Klokkeslett på formen HHmm eller HHmmss for når kjøringen senest avsluttes."
    )
    public void settSluttid(final String value) {
        new LocalTimeValidator().validate("sluttid", value, spec);
        sluttidspunkt = new LocalTimeConverter().convert(value);
    }

    @Option(names = {"-slettLog"},
            description = "Sletter alle batch-kataloger i -log katalogen som er eldre enn n antall dager dersom n > 0."
    )
    int slettLogEldreEnn = 0;

    @Override
    public String toString() {
        return Stream.concat(
                Stream.of(ArgumentSummary.createParameterSummary(this)),
                Stream.of("ADVARSEL: Antall noder angitt er lik antall prosessorer på serveren.")
                        .filter(advarsel -> nodes.equals(availableProcessors()))
        )
                .collect(joining("\n"))
                .replaceAll("antall prosessorar ", "");
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public String getGrunnlagsdataBatchId() {
        return uttrekk.toString();
    }

    public int getFraAar() {
        return fraAar;
    }

    public int getTilAar() {
        return tilAar;
    }

    public Path getUtkatalog() {
        return utkatalog;
    }

    public Path getLogkatalog() {
        return logkatalog;
    }

    public Path getInnkatalog() {
        return innkatalog;
    }

    public Path getGrunnlagsdataBatchKatalog() {
        return uttrekkskatalog();
    }

    @Override
    public Tidsseriemodus modus() {
        return modus.modus();
    }

    public Duration getKjoeretid() {
        return DurationUtil.convert(kjoeretid).get();
    }

    public LocalTime getSluttidspunkt() {
        return sluttidspunkt;
    }

    public int getSlettLogEldreEnn() {
        return slettLogEldreEnn;
    }

    @Override
    public boolean hjelp() {
        return hjelp;
    }

    @Override
    public Path uttrekkskatalog() {
        return uttrekk.resolve(innkatalog);
    }

    @Override
    public Path logkatalog() {
        return logkatalog;
    }

    @Override
    public Path utkatalog() {
        return utkatalog;
    }

    @Override
    public AntallProsessorar antallProsessorar() {
        return nodes;
    }

    public void antallProsessorar(final AntallProsessorar antall) {
        this.nodes = requireNonNull(antall, "antall er påkrevd, men var null");
    }

    @Override
    public AldersgrenseForSlettingAvLogKatalogar slettegrense() {
        return aldersgrenseForSlettingAvLogKatalogar(slettLogEldreEnn);
    }

    @Override
    public Duration maksimalKjøretid() {
        //noinspection OptionalGetWithoutIsPresent
        return DurationUtil.convert(kjoeretid).get();
    }

    @Override
    public LocalTime avsluttFørTidspunkt() {
        return sluttidspunkt;
    }

    @Override
    public void velgUttrekkVissIkkeAngitt(final Function<Path, UttrekksId> utvelger) {
        if (uttrekk == null) {
            this.uttrekk = utvelger.apply(innkatalog);
        }
    }

    /**
     * @since 1.1.0
     */
    public void registrer(final ServiceRegistry registry) {
        registry.registerService(Observasjonsperiode.class, observasjonsperiode());
    }

    Observasjonsperiode observasjonsperiode() {
        return new Observasjonsperiode(
                new Aarstall(fraAar).atStartOfYear(),
                new Aarstall(tilAar).atEndOfYear()
        );
    }

    @Override
    public List<String> argumenter() {
        return Arrays.asList(
                String.format("b: %s", beskrivelse),
                String.format("kjoeretid: %s", kjoeretid),
                String.format("sluttid: %s", sluttidspunkt),
                String.format("i: %s", innkatalog != null ? innkatalog.toString() : ""),
                String.format("o: %s", utkatalog != null ? utkatalog.toString() : ""),
                String.format("log: %s", logkatalog != null ? logkatalog.toString() : ""),
                String.format("fraAar: %d", fraAar),
                String.format("tilAar: %d", tilAar),
                String.format("n: %s", antallNoderForPrinting),
                String.format("m: %s", modus != null ? modus.toString() : ""),
                String.format("id: %s", uttrekk),
                String.format("slettLog: %d", slettLogEldreEnn)
        );
    }
}
