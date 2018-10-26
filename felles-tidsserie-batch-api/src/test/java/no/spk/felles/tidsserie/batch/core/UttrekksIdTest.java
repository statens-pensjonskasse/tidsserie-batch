package no.spk.felles.tidsserie.batch.core;

import static java.util.stream.Collectors.joining;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.uttrekksId;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.velgNyeste;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.OptionalAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UttrekksIdTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void skal_kun_godta_gyldige_navn_på_uttrekk() {
        assertUttrekksId("grunnlagsdata_").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("_1970-01-01_00-00-00-00").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_0-0-0-0").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_00-00-00").isInstanceOf(IllegalArgumentException.class);
        assertUttrekksId("grunnlagsdata_1970-01-01_99-99-99-99").isInstanceOf(IllegalArgumentException.class);

        assertUttrekksId("grunnlagsdata_1970-01-01_00-00-00-00").doesNotThrowAnyException();
        assertUttrekksId("grunnlagsdata_2018-03-31_01-23-59-99").doesNotThrowAnyException();
    }

    @Test
    public void skal_ikke_velge_nyeste_uttrekk_dersom_det_ikke_eksisterer_noen_kandidater() {
        assertVelgNyeste().isEmpty();
    }

    @Test
    public void skal_ikke_velge_et_uttrekk_dersom_kandidaten_ikke_er_en_katalog() {
        assertVelgNyeste(
                katalog("grunnlagsdata_1970-01-01_00-00-00-00"),
                fil("grunnlagsdata_2018-01-01_23-59-59-00")
        )
                .contains(uttrekksId("grunnlagsdata_1970-01-01_00-00-00-00"));
    }

    @Test
    public void skal_ikke_velge_et_uttrekk_dersom_navnet_på_kandidaten_ikke_starter_på_grunnlagsdata_() {
        assertVelgNyeste(
                katalog("uttrekk_1970-01-01_00-00-00-00"),
                katalog("grunnlatsdata_1970-01-01_00-00-00-00"),
                katalog("1970-01-01_00-00-00-00")
        )
                .isEmpty();
    }

    @Test
    public void skal_ikke_velge_et_uttrekk_dersom_navnet_på_kandidaten_ikke_slutter_med_et_tidspunkt() {
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
    public void skal_velge_det_nyeste_uttrekket_basert_på_tidspunkt_i_navnet_til_kandidatene() {
        assertVelgNyeste(
                katalog("grunnlagsdata_2120-01-01_00-00-00-99"),
                katalog("grunnlagsdata_1970-01-01_00-00-00-00"),
                katalog("grunnlagsdata_2120-01-01_00-00-00-98"),
                katalog("grunnlagsdata_2018-01-01_00-00-00-00")
        )
                .contains(uttrekksId("grunnlagsdata_2120-01-01_00-00-00-99"));
    }

    @Test
    public void skal_returnere_full_sti_til_uttrekket() {
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
            return temp.newFile(fil).toPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path katalog(final String katalog) {
        try {
            return temp.newFolder(katalog).toPath();
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
}