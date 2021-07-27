package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.uttrekksId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import no.spk.felles.tidsserie.batch.core.UttrekksId;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class TidsserieArgumentsFactoryTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final ModusRule modusar = new ModusRule();

    private final TidsserieArgumentsFactory parser = new TidsserieArgumentsFactory();

    private final String defaultModus = "modus";

    private Path innKatalog;
    private Path logKatalog;
    private Path utKatalog;

    @Before
    public void _before() {
        modusar.support(defaultModus);

        innKatalog = newFolder("inn");
        logKatalog = newFolder("log");
        utKatalog = newFolder("ut");
    }

    @Test
    public void skal_kreve_verdi_for_modus_parameter() {
        assertFeilFraParsingAvArgumenter(
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-b", name.getMethodName()
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Følgende valg er påkrevd: -m");
    }

    @Test
    public void skalAvviseUgyldigeTidsseriemodusar() {
        assertFeilFraParsingAvArgumenter(
                "-m", "lol",
                "-i", innKatalog,
                "-o", utKatalog,
                "-log", logKatalog,
                "-b", name.getMethodName()
        )
                .isInstanceOf(UgyldigKommandolinjeArgumentException.class)
                .hasMessageContaining("Modus 'lol' er ikkje støtta")
                .hasMessageContaining("Følgjande modusar er støtta:")
                .hasMessageContaining("- modus");
    }

    @Test
    public void testBeskrivelseInputOutputRequired() {
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
    public void testArgsMedFraAarFoerTilAarThrowsException() {
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
    public void testInvalidKjoeretidLettersThrowsException() {
        testInvalidKjoeretid("abcd");
    }

    @Test
    public void testInvalidKjoeretidNumbersThrowsException() {
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
    public void testInvalidSluttidWithlLettersThrowsException() {
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
    public void testArgsMedBeskrivelseOgHjelpThrowsUsageRequestedException() {
        assertFeilFraParsingAvArgumenter("-help").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-hjelp").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-h").isInstanceOf(BruksveiledningSkalVisesException.class);
        assertFeilFraParsingAvArgumenter("-?").isInstanceOf(BruksveiledningSkalVisesException.class);
    }

    @Test
    public void skal_feile_dersom_ingen_uttrekkskatalogar_eksisterer() {
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
    public void skal_automatisk_velge_nyeste_uttrekkskatalog_dersom_uttrekk_ikke_er_angitt() {
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
            return testFolder.newFolder(navn).toPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}