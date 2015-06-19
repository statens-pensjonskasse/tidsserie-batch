package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;

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
            description="Batchen vil lage en ny katalog i arbeidskatalogen hvor resultatet av kjøringen vil bli lagret.",
            validateWith = PathStringValidator.class,
            validateValueWith = WritablePathValidator.class,
            required = true
    )
    Path utkatalog;

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
            description = "Antall noder som skal brukes for å utgjøre grid for tidsserie-prossesering. Default er lik antall prosessorer i kjøremiljøet.",
            validateWith = IntegerValidator.class,
            validateValueWith = GreaterThanZeroValidator.class)
    int nodes = Runtime.getRuntime().availableProcessors();

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

    public Path getInnkatalog() {
        return innkatalog;
    }

    public Path getGrunnlagsdataBatchKatalog() {
        return innkatalog.resolve(grunnlagsdataBatchId);
    }

    public int getNodes() {
        return nodes;
    }
}
