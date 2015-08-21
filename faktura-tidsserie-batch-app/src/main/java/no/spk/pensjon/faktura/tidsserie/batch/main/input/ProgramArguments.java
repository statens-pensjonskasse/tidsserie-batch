package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;

import com.beust.jcommander.Parameter;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ProgramArguments {
    @Parameter(names = {"-hjelp", "?", "-h", "-help" }, help = true,
            description = "Printer denne oversikten.")
    boolean hjelp;

    @Parameter(names = { "-b" },
            description = "Tekstlig beskrivelse av hensikten med kjøringen.",
            required = true)
    String beskrivelse;

    @Parameter(names = { "-i" },
            description="En katalog som inneholder resultatkatalog fra grunnlagsdata-batch.",
            validateWith = PathStringValidator.class,
            validateValueWith = ReadablePathValidator.class,
            required = true
    )
    Path innkatalog;

    @Parameter(names = { "-id" },
            validateWith = BatchIdValidator.class,
            description = "Batch-id som identifiserer en resultat-katalog produsert av grunnlagsdata-batch i inn-katalogen. Dersom -id mangler ser denne batchen etter nyeste resulatkatalog.")
    String grunnlagsdataBatchId;

    @Parameter(names = { "-o" },
            description="Katalogen hvor tidsserie.csv og ok.trg fil blir lagret",
            validateWith = PathStringValidator.class,
            validateValueWith = WritablePathValidator.class,
            required = true
    )
    Path utkatalog;

    @Parameter(names = { "-log" },
            description="Batchen vil lage en ny katalog i arbeidskatalogen hvor log og metadata for kjøringen vil bli lagret.",
            validateWith = PathStringValidator.class,
            validateValueWith = WritablePathValidator.class,
            required = true
    )
    Path logkatalog;

    @Parameter(names = {"-fraAar"},
            description="Medlemsdata hentes fra og med 01.01 i angitt år.",
            validateWith = IntegerValidator.class,
            validateValueWith = YearValidator.class)
    int fraAar = 2015;

    @Parameter(names = {"-tilAar"},
            description="Medlemsdata hentes til og med 31.12 i angitt år.",
            validateWith = IntegerValidator.class,
            validateValueWith = YearValidator.class)
    int tilAar = 2015;

    @Parameter(names = { "-n" },
            description = "Antall noder som skal brukes for å utgjøre grid for tidsserie-prossesering. Default er lik antall prosessorer på serveren minus 1.",
            validateWith = IntegerValidator.class,
            validateValueWith = NodeCountValidator.class)
    int nodes = Runtime.getRuntime().availableProcessors() - 1;

    @Parameter(names = { "-m" },
            description = "Modusen batchen skal bruke for oppbygging av og lagring av tidsserien.",
            validateWith = ModusValidator.class,
            converter = ModusConverter.class
    )
    Modus modus = Modus.STILLINGSFORHOLD_OBSERVASJONAR;

    @Parameter(names = "-kjoeretid",
            description = "Maks kjøretid på formatet HHmm.",
            validateWith = DurationValidator.class)
    String kjoeretid = "0400";

    @Parameter(names = { "-sluttid" },
            description = "Klokkeslett på formen HH:mm eller HH:mm:ss for når kjøringen senest avsluttes.",
            validateWith = LocalTimeValidator.class,
            converter = LocalTimeConverter.class)
    LocalTime sluttidspunkt = LocalTime.parse("23:59");

    @Parameter(names = { "-slett" },
            description = "Sletter alle batch-kataloger og innhold som er eldre enn n antall dager dersom n > 0.")
    int slettEldreEnn = 0;

    public boolean isHjelp() {
        return hjelp;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public String getGrunnlagsdataBatchId() {
        return grunnlagsdataBatchId;
    }

    public void setGrunnlagsdataBatchId(String grunnlagsdataBatchId) {
        this.grunnlagsdataBatchId = grunnlagsdataBatchId;
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
        return innkatalog.resolve(grunnlagsdataBatchId);
    }

    public int getNodes() {
        return nodes;
    }

    public Optional<String> postMessage() {
        if (nodes == Runtime.getRuntime().availableProcessors()) {
            return Optional.of("ADVARSEL: Antall noder angitt er lik antall prosessorer på serveren.");
        }
        return Optional.empty();
    }

    public Tidsseriemodus modus() {
        return modus.modus();
    }

    public String getKjoeretid() {
        return kjoeretid;
    }

    public LocalTime getSluttidspunkt() {
        return sluttidspunkt;
    }

    public int getSlettEldreEnn() {
        return slettEldreEnn;
    }
}
