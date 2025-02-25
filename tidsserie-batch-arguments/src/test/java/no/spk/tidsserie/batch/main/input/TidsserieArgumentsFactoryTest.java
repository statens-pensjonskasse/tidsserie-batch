package no.spk.tidsserie.batch.main.input;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static no.spk.tidsserie.batch.core.UttrekksId.uttrekksId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

import no.spk.tidsserie.batch.core.UttrekksId;
import no.spk.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
public class TidsserieArgumentsFactoryTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @TempDir
    public File testFolder;

    public String name;

    @RegisterExtension
    public final ModusExtension modusar = new ModusExtension();

    private final TidsserieArgumentsFactory parser = new TidsserieArgumentsFactory();

    private final String defaultModus = "modus";

    private Path innKatalog;
    private Path logKatalog;
    private Path utKatalog;

    @BeforeEach
    void _before(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        testMethod.ifPresentOrElse(method -> this.name = method.getName(), () -> this.name = "ukjentMetodenavn");
        modusar.support(defaultModus);

        innKatalog = newFolder("inn");
        logKatalog = newFolder("log");
        utKatalog = newFolder("ut");
    }

    @Test
    void skal_kreve_verdi_for_modus_parameter() {
        assertFeilFraParsingAvArgumenter(
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-b", name
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Følgende valg er påkrevd: -m");
    }

    @Test
    void skalAvviseUgyldigeTidsseriemodusar() {
        assertFeilFraParsingAvArgumenter(
                "-m", "lol",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-b", name
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Modus 'lol' er ikkje støtta")
                .hasMessageContaining("Følgjande modusar er støtta:")
                .hasMessageContaining("- modus");
    }

    @Test
    void beskrivelseInputOutputRequired() {
        assertFeilFraParsingAvArgumenter()
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Følgende valg er påkrevd:")
                .hasMessageContaining("-i")
                .hasMessageContaining("-b")
                .hasMessageContaining("-m")
                .hasMessageContaining("-o")
                .hasMessageContaining("-log");
    }

    @Test
    void argsMedFraAarFoerTilAarThrowsException() {
        assertFeilFraParsingAvArgumenter(
                "-b", "test",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-m", defaultModus,
                "-fraAar", "2009",
                "-tilAar", "2008"
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("'-fraAar' kan ikke være større enn '-tilAar'")
                .hasMessageContaining("2009 > 2008");
    }

    @Test
    void invalidKjoeretidLettersThrowsException() {
        testInvalidKjoeretid("abcd");
    }

    @Test
    void invalidKjoeretidNumbersThrowsException() {
        testInvalidKjoeretid("123");
    }

    private void testInvalidKjoeretid(String kjoeretid) {
        assertFeilFraParsingAvArgumenter(
                "-b", "test",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-kjoeretid", kjoeretid
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("kjoeretid");
    }

    @Test
    void invalidSluttidWithlLettersThrowsException() {
        assertFeilFraParsingAvArgumenter(
                "-b", "test",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-sluttid", "a"
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("sluttid");
    }

    @Test
    void argsMedBeskrivelseOgHjelpThrowsUsageRequestedException() {
        assertFeilFraParsingAvArgumenter("-help").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-hjelp").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-h").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-?").isInstanceOf(BruksveiledningSkalVisesException.class);
    }

    @Test
    void skal_feile_dersom_ingen_uttrekkskatalogar_eksisterer() {
        assertFeilFraParsingAvArgumenter(
                "-b", "Test batch id missing",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-m", defaultModus
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Det finnes ingen underkataloger med uttrekk av grunnlagsdata i ")
                .hasMessageContaining(innKatalog.toString())
        ;

        final UttrekksId uttrekksId = uttrekksId("grunnlagsdata_2015-01-01_01-00-00-00");
        assertFeilFraParsingAvArgumenter(
                "-b", "Test batch id missing",
                "-id", uttrekksId,
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-m", defaultModus
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Feil i parameter: Filen ")
                .hasMessageContaining(uttrekksId.resolve(innKatalog).toString())
                .hasMessageContaining("eksisterer ikke, verifiser at du har angitt rett filsti.");
    }

    @Test
    void skal_automatisk_velge_nyeste_uttrekkskatalog_dersom_uttrekk_ikke_er_angitt() {
        final Path expected = nyttUttrekk(uttrekksId("grunnlagsdata_2999-01-01_01-00-00-01"));
        nyttUttrekk(uttrekksId("grunnlagsdata_2015-01-01_01-00-00-00"));

        assertParse(
                "-b", "Test set default batch id",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-m", defaultModus
        )
                .satisfies(
                        arguments -> assertThat(arguments.uttrekkskatalog())
                                .as("uttrekkskatalogen")
                                .isEqualTo(expected)
                );

    }

    private ObjectAssert<ProgramArguments> assertParse(final Object... args) throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException {
        return assertThat(parse(args(args)))
                .as(
                        "%s.parse(%s)",
                        parser.getClass().getSimpleName(),
                        join(" ", args(args))
                );
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertFeilFraParsingAvArgumenter(final Object... arguments) {
        return softly.assertThatCode(() -> parse(args(arguments)))
                .as(
                        "%s.parse(%s)",
                        parser.getClass().getSimpleName(),
                        join("\n", args(arguments))
                );
    }

    private ProgramArguments parse(final String... args) throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException {
        final TidsserieBatchArgumenter argumenter = parser.parse(args);
        assertThat(argumenter).isInstanceOf(ProgramArguments.class);
        return (ProgramArguments) argumenter;
    }

    private static String[] args(final Object... args) {
        return stream(args)
                .map(Object::toString)
                .toArray(String[]::new);
    }

    private Path nyttUttrekk(final UttrekksId uttrekk) {
        final Path katalog = uttrekk.resolve(innKatalog);
        assert katalog.toFile().mkdirs() : "Klarte ikkje opprette katalogen " + katalog;
        return katalog;
    }

    private Path newFolder(final String navn) {
        try {
            return newFolder(testFolder, navn).toPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}