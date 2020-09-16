package no.spk.felles.tidsserie.batch.core.grunnlagsdata.csv;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import no.spk.felles.tidsperiode.Tidsperiode;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class CSVInputTest {
    private static final String DUMMYDATA = "1;2;3;4;5;6;7;8;9;0";

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final TemporaryFolder folder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private CSVInput fixture;

    private File medlemsdata;

    private File baseDir;

    @Before
    public void _before() throws IOException {
        baseDir = folder.newFolder(name.getMethodName());

        medlemsdata = newTemporaryFile("medlemsdata.csv.gz");

        fixture = new CSVInput(baseDir.toPath());
    }

    @Test
    public void skal_kreve_innkatalog_ved_konstruksjon() {
        assertThatCode(
                () ->new CSVInput(null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("innkatalog er påkrevd, men var null");
    }

    /**
     * Verifiserer at medlemsdatafila blir lukka når straumen med innleste linjer blir lukka.
     */
    @Test
    public void skalLukkeMedlemsdatafilVedLukkingAvStream() throws IOException {
        write(medlemsdata, of(DUMMYDATA));

        try (final Stream<List<String>> medlemsdata = fixture.medlemsdata()) {
            assertThat(medlemsdata.count()).isEqualTo(1);
        }

        assertDeletable(this.medlemsdata).isTrue();
    }

    /**
     * Verifiserer at alle filene med referansedata blir lukka etter at straumen med tidsperioder lest frå filene
     * blir lukka.
     */
    @Test
    public void skalLukkeReferansefilerVedLukkingAvStream() throws IOException {
        fixture.addOversettere(new FakeOversetter());

        final File referanse1 = newTemporaryFile("referansedata1.csv.gz");
        final File referanse2 = newTemporaryFile("referansedata2.csv.gz");

        write(referanse1, of(DUMMYDATA));
        write(referanse2, of(DUMMYDATA));

        try (final Stream<Tidsperiode<?>> referansedata = fixture.referansedata()) {
            assertThat(referansedata.count()).as("total line count").isEqualTo(2);
        }

        assertDeletable(this.medlemsdata).isTrue();
        assertDeletable(referanse1).isTrue();
        assertDeletable(referanse2).isTrue();
    }

    /**
     * Verifiserer at CSV-fila med medlemsdata ikkje blir lest inn og tatt tatt med som en del av referansedatane.
     */
    @Test
    public void skalEkskludereMedlemsdataFraReferansedataFilListe() throws IOException {
        try (final Stream<Path> referansedatafiler = fixture.referansedataFiler()) {
            assertThat(
                    referansedatafiler
                            .map(path -> path.getFileName().toString())
                            .collect(toList())
            ).doesNotContain("medlemsdata.csv.gz");
        }
    }

    @Test
    public void skal_støtte_ukomprimerte_csv_filer() throws IOException {
        write(newTemporaryFile("tjafs.csv"), of(DUMMYDATA));

        softly.assertThat(
                fixture.referansedataFiler()
        )
                .as("referansedatafiler skal inkludere ukomprimerte CSV-filer")
                .hasSize(1)
                .extracting(e -> e.toFile().getName())
                .contains("tjafs.csv");

        fixture.addOversettere(new FakeOversetter());
        softly.assertThat(
                fixture.referansedata()
        )
                .as("referansedata frå tjafs.csv")
                .hasSize(1);

        assertDeletable(medlemsdata).isTrue();

        write(newTemporaryFile("medlemsdata.csv"), of(DUMMYDATA));
        softly.assertThat(
                fixture.medlemsdata()
        )
                .as("medlemsdata frå medlemsdata.csv")
                .hasSize(1);
    }

    @Test
    public void skal_ikkje_godta_filer_som_eksisterer_i_både_komprimert_og_ukomprimert_versjon() throws IOException {
        write(newTemporaryFile("filnavn.csv.gz"), of(DUMMYDATA));
        write(newTemporaryFile("filnavn.csv"), of(DUMMYDATA));

        //noinspection ResultOfMethodCallIgnored
        softly.assertThatCode(
                () -> fixture
                        .referansedata()
                        .findAny()
        )
                .as("referansedata for CSV-fil som eksisterer i både ukomprimert og komprimert versjon")
                .isInstanceOf(DuplisertCSVFilException.class);

        assertDeletable(medlemsdata).isTrue();

        write(newTemporaryFile("medlemsdata.csv.gz"), of(DUMMYDATA));
        write(newTemporaryFile("medlemsdata.csv"), of(DUMMYDATA));

        //noinspection ResultOfMethodCallIgnored
        softly.assertThatCode(
                () -> fixture
                        .medlemsdata()
                        .findAny()
        )
                .as("medlemsdata som eksisterer i både ukomprimert og komprimert versjon")
                .isInstanceOf(DuplisertCSVFilException.class);
    }

    private File newTemporaryFile(final String filename) throws IOException {
        final File file = new File(baseDir, filename);
        assertThat(file.createNewFile()).as("was " + file + " successfully created?").isTrue();
        return file;
    }

    private static AbstractBooleanAssert<?> assertDeletable(final File file) {
        return assertThat(file.delete())
                .as("er " + file + " sletta?");
    }

    private static void write(final File file, final Stream<String> lines) throws IOException {
        try (final OutputStream output = open(file)) {
            lines.forEach(line -> write(output, line));
        }
    }

    private static OutputStream open(final File file) throws IOException {
        final FileOutputStream output = new FileOutputStream(file);
        if (file.getName().endsWith("gz")) {
            return new GZIPOutputStream(output);
        }
        return output;
    }

    private static void write(final OutputStream output, final String line) {
        try {
            output.write(line.getBytes());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
