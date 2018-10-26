package no.spk.felles.tidsserie.batch.core.grunnlagsdata.csv;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

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
import no.spk.felles.tidsserie.batch.util.TemporaryFolderWithDeleteVerification;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.junit.MockitoRule;

public class CSVInputTest {
    public static final String DUMMYDATA = "1;2;3;4;5;6;7;8;9;0";

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final TemporaryFolder folder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

    @Rule
    public final ExpectedException e = ExpectedException.none();

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
        e.expect(NullPointerException.class);
        e.expectMessage("innkatalog er påkrevd, men var null");

        new CSVInput(null);
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
        try (final OutputStream output = new GZIPOutputStream(new FileOutputStream(file))) {
            lines.forEach(line -> write(output, DUMMYDATA));
        }
    }

    private static void write(final OutputStream output, final String line) {
        try {
            output.write(line.getBytes());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}