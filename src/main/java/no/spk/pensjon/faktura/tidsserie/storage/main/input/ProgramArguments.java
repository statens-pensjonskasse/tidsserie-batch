package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;

import com.beust.jcommander.Parameter;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ProgramArguments {
    @Parameter(names = {"-hjelp", "?", "-h", "-help" }, help = true)
    boolean hjelp;

    @Parameter(names = { "-beskrivelse", "-b" },
            description = "Tekstlig beskrivelse av hensikten med kjøringen.",
            required = true)
    String beskrivelse;

    @Parameter(names = { "-id" },
            validateWith = BatchIdValidator.class,
            description = "Batch-id som identifiserer en resultat-katalog produsert av grunnlagsdata-batch i inn-katalogen. Dersom -id mangler ser denne batchen etter nyeste resulatkatalog.")
    String grunnlagsdataBatchId;

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

    @Parameter(names = { "-ut", "-o" },
            description="Batchen vil lage en ny katalog i arbeidskatalogen hvor resultatet av kjøringen vil bli lagret.",
            validateValueWith = WritablePathValidator.class
    )
    Path utkatalog = Paths.get("target/");

    @Parameter(names = { "-inn", "-i" },
            description="En katalog som inneholder resultatkatalog fra grunnlagsdata-batch.",
            validateValueWith = WritablePathValidator.class
    )
    Path innkatalog = Paths.get("../faktura-grunnlagsdata-batch/target/");

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

}
