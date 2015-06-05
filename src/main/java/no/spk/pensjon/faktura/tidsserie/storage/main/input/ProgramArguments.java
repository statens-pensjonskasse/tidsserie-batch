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
            description = "Batch-id som identifiserer en resultat-katalog produsert av grunnlagsdata-batch i inn-katalogen. Dersom -id ser denne batchen etter nyeste resulatkatalog.")
    String batchId;

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

    @Parameter(names = { "-fraAvtale" },
            description="Medlemsdata hentes kun i intervallet [fraAvtale, tilAvtale].",
            validateWith = IntegerValidator.class,
            validateValueWith = GreaterThanZeroValidator.class)
    long fraAvtale = 100000;

    @Parameter(names = { "-tilAvtale" },
            description="Medlemsdata hentes kun i intervallet [fraAvtale, tilAvtale].",
            validateWith = IntegerValidator.class,
            validateValueWith = GreaterThanZeroValidator.class)
    long tilAvtale = 999999;

    @Parameter(names = { "-ut", "-o" },
            description="Batchen vil lage en ny katalog i arbeidskatalogen hvor resultatet av kjøringen vil bli lagret.",
            validateValueWith = WritablePathValidator.class
    )
    Path utkatalog = Paths.get("target/");

    @Parameter(names = { "-inn", "-i" },
            description="En katalog som inneholder resultatkatalog fra grunnlagsdata-batch.",
            validateValueWith = WritablePathValidator.class
    )
    Path innkatalog = Paths.get("target/");

    @Parameter(names = "-kjoeretid",
            description = "Maks kjøretid på formatet HHmm.",
            validateWith = DurationValidator.class)
    String kjoeretid = "0200";

    @Parameter(names = { "-sluttid" },
            description = "Klokkeslett på formen HH:mm eller HH:mm:ss for når kjøringen senest avsluttes.",
            validateWith = LocalTimeValidator.class,
            converter = LocalTimeConverter.class)
    LocalTime sluttidspunkt = LocalTime.parse("23:59");
}
