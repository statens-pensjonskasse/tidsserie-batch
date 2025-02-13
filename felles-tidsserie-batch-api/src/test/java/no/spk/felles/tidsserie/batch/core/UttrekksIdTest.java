package no.spk.felles.tidsserie.batch.core;

import static java.util.stream.Collectors.joining;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.uttrekksId;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.velgNyeste;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.OptionalAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
public class UttrekksIdTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @TempDir
    public File temp;

    @Test
    void skal_kun_godta_gyldige_navn_på_uttrekk() {
        assertUttrekksId("grunnlagsdata_").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("_1970-01-01_00-00-00-00").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_0-0-0-0").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_00-00-00").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_99-99-99-99").isInstanceOf(IllegalArgumentException.class);

        assertUttrekksId("grunnlagsdata_1970-01-01_00-00-00-00").doesNotThrowAnyException();
        assertUttrekksId("grunnlagsdata_2018-03-31_01-23-59-99").doesNotThrowAnyException();
    }

    @Test
    void skal_ikke_velge_nyeste_uttrekk_dersom_det_ikke_eksisterer_noen_kandidater() {
        assertVelgNyeste().isEmpty();
    }

    @Test
    void skal_ikke_velge_et_uttrekk_dersom_kandidaten_ikke_er_en_katalog() {
        assertVelgNyeste(
                katalog("grunnlagsdata_1970-01-01_00-00-00-00"),
                fil("grunnlagsdata_2018-01-01_23-59-59-00")
        )
                .contains(uttrekksId("grunnlagsdata_1970-01-01_00-00-00-00"));
    }

    @Test
    void skal_ikke_velge_et_uttrekk_dersom_navnet_på_kandidaten_ikke_starter_på_grunnlagsdata_() {
        assertVelgNyeste(
                katalog("uttrekk_1970-01-01_00-00-00-00"),
                katalog("grunnlatsdata_1970-01-01_00-00-00-00"),
                katalog("1970-01-01_00-00-00-00")
        )
                .isEmpty();
    }

    @Test
    void skal_ikke_velge_et_uttrekk_dersom_navnet_på_kandidaten_ikke_slutter_med_et_tidspunkt() {
        assertVelgNyeste(
                katalog("grunnlagsdata_1970-01-01"),
                katalog("grunnlagsdata_1970-01-01_00-00"),
                katalog("grunnlagsdata_1970-01-01_0-0-0-0"),
                katalog("grunnlagsdata_1970-01-01_00-00-00"),
                katalog("grunnlagsdata_1970-01-01_99-99-99-99-")
        )
                .isEmpty();
    }

    @Test
    void skal_velge_det_nyeste_uttrekket_basert_på_tidspunkt_i_navnet_til_kandidatene() {
        assertVelgNyeste(
                katalog("grunnlagsdata_2120-01-01_00-00-00-99"),
                katalog("grunnlagsdata_1970-01-01_00-00-00-00"),
                katalog("grunnlagsdata_2120-01-01_00-00-00-98"),
                katalog("grunnlagsdata_2018-01-01_00-00-00-00")
        )
                .contains(uttrekksId("grunnlagsdata_2120-01-01_00-00-00-99"));
    }

    @Test
    void skal_returnere_full_sti_til_uttrekket() {
        final Path innKatalog = katalog("inn");
        assertThat(
                uttrekksId("grunnlagsdata_2120-01-01_00-00-00-99")
                        .resolve(innKatalog)
        )
                .isAbsolute()
                .hasToString(
                        innKatalog
                                .resolve("grunnlagsdata_2120-01-01_00-00-00-99")
                                .toString()
                );
    }

    private Path fil(final String fil) {
        try {
            return File.createTempFile(fil, null, temp).toPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path katalog(final String katalog) {
        try {
            return newFolder(temp, katalog).toPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private OptionalAssert<UttrekksId> assertVelgNyeste(final Path... kandidater) {
        return assertThat(velgNyeste(Stream.of(kandidater)))
                .as(
                        "%s.velgNyeste(...) med følgende kandidater:\n%s",
                        UttrekksId.class.getSimpleName(),
                        Stream.of(kandidater)
                                .map(Object::toString)
                                .sorted()
                                .map(kandidat -> "- " + kandidat)
                                .collect(joining("\n"))
                );
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertUttrekksId(final String uttrekksId) {
        return softly.assertThatCode(() -> uttrekksId(uttrekksId))
                .as(
                        "Feil fra %s.uttrekksId('%s')",
                        UttrekksId.class.getSimpleName(),
                        uttrekksId
                );
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