package no.spk.felles.tidsserie.batch.main.input;

import static java.time.LocalDate.now;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
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
import no.spk.faktura.input.ReadablePathValidator;
import no.spk.faktura.input.WritableDirectoryValidator;
import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.UttrekksId;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import com.beust.jcommander.Parameter;

/**
 * @author Snorre E. Brekke - Computas
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class ProgramArguments implements Arguments, TidsserieBatchArgumenter {
    @Parameter(names = { "-hjelp", "-?", "-h", "-help" },
            help = true,
            description = "Viser denne brukerveiledningen.")
    boolean hjelp;

    @Parameter(names = { "-b" },
            description = "Tekstlig beskrivelse av hensikten med kjøringen.",
            required = true)
    String beskrivelse;

    @Parameter(names = { "-i" },
            description = "En katalog som inneholder 1 eller flere uttrekkskataloger som batchen kan benytte for innlesing av grunnlagsdata.",
            validateWith = PathStringValidator.class,
            validateValueWith = ReadablePathValidator.class,
            required = true
    )
    Path innkatalog;

    @Parameter(names = { "-id" },
            validateWith = BatchIdValidator.class,
            description = "En tekstlig kode som identifiserer hvilken uttrekkskatalog i inn-katalogen som kjøringen skal benytte. Dersom -id mangler benyttes det nyeste uttrekket.",
            converter = UttrekksIdConverter.class
    )
    UttrekksId uttrekk;

    @Parameter(names = { "-o" },
            description = "Utkatalogen der resultatet av kjøringen blir lagret.",
            validateWith = PathStringValidator.class,
            validateValueWith = WritableDirectoryValidator.class,
            required = true
    )
    Path utkatalog;

    @Parameter(names = { "-log" },
            description = "Batchen vil lage en ny katalog i -log katalogen hvor batch.log og metadata for kjøringen vil bli lagret.",
            validateWith = PathStringValidator.class,
            validateValueWith = WritableDirectoryValidator.class,
            required = true
    )
    Path logkatalog;

    @Parameter(names = { "-fraAar" },
            description = "Tidsserien lages fra og med 01.01 i angitt år.",
            validateWith = IntegerValidator.class,
            validateValueWith = YearValidator.class)
    int fraAar = new StandardBatchperiode(now()).fraAar();

    @Parameter(names = { "-tilAar" },
            description = "Tidsserien lages til og med 31.12 i angitt år.",
            validateWith = IntegerValidator.class,
            validateValueWith = YearValidator.class)
    int tilAar = new StandardBatchperiode(now()).tilAar();

    @Parameter(names = { "-n" },
            description = "Antall noder som skal brukes for å utgjøre grid for tidsserie-prossesering. Default er lik antall prosessorer på serveren minus 1.",
            validateWith = AntallProsessorarValidator.class,
            converter = AntallProsessorarConverter.class
    )
    AntallProsessorar nodes = AntallProsessorar.standardAntallProsessorar();

    @Parameter(names = { "-m" },
            description = "Modusen batchen skal bruke for oppbygging av og lagring av tidsserien.",
            validateWith = ModusValidator.class,
            converter = ModusConverter.class,
            required = true
    )
    Modus modus;

    @Parameter(names = "-kjoeretid",
            description = "Maks kjøretid på formatet HHmm.",
            validateWith = DurationValidator.class)
    String kjoeretid = "0400";

    @Parameter(names = { "-sluttid" },
            description = "Klokkeslett på formen HHmm eller HHmmss for når kjøringen senest avsluttes.",
            validateWith = LocalTimeValidator.class,
            converter = LocalTimeConverter.class)
    LocalTime sluttidspunkt = LocalTime.parse("23:59");

    @Parameter(names = { "-slettLog" },
            description = "Sletter alle batch-kataloger i -log katalogen som er eldre enn n antall dager dersom n > 0.")
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
}
