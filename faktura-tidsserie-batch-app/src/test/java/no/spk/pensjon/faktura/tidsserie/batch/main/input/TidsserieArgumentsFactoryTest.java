package no.spk.pensjon.faktura.tidsserie.batch.main.input;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TidsserieArgumentsFactoryTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final ModusRule modusar = new ModusRule();

    private final String defaultModus = "modus";

    @Before
    public void _before() {
        modusar.support(defaultModus);
    }

    @Test
    public void skal_kreve_verdi_for_modus_parameter() throws IOException {
        exception.expect(InvalidParameterException.class);
        exception.expectMessage("Følgende valg er påkrevd: -m");

        final String path = createTestFolders();

        new TidsserieArgumentsFactory().create(
                "-i", path,
                "-o", path,
                "-log", path,
                "-b", name.getMethodName()
        );
    }

    @Test
    public void skalAvviseUgyldigeTidsseriemodusar() throws IOException {
        exception.expect(InvalidParameterException.class);
        exception.expectMessage("Modus");

        final String path = createTestFolders();
        new TidsserieArgumentsFactory().create(
                "-m", "lol",
                "-log", path,
                "-i", path,
                "-o", path,
                "-b", name.getMethodName()
        );
    }

    @Test
    public void testBeskrivelseInputOutputRequired() throws Exception {
        exception.expect(InvalidParameterException.class);
        exception.expectMessage("Følgende valg er påkrevd: -log -o -i -m -b");

        new TidsserieArgumentsFactory().create();
    }

    @Test
    public void testArgsMedFraAarFoerTilAarThrowsException() throws Exception {
        String path = createTestFolders();

        exception.expect(InvalidParameterException.class);
        exception.expectMessage("'-fraAar' kan ikke være større enn '-tilAar'");
        exception.expectMessage("2009 > 2008");

        new TidsserieArgumentsFactory().create(
                "-b", "test",
                "-o", path,
                "-i", path,
                "-log", path,
                "-m", defaultModus,
                "-fraAar", "2009",
                "-tilAar", "2008"
        );
    }

    @Test
    public void testInvalidKjoeretidLettersThrowsException() throws Exception {
        testInvalidKjoeretid("abcd");
    }

    @Test
    public void testInvalidKjoeretidNumbersThrowsException() throws Exception {
        testInvalidKjoeretid("123");
    }

    private void testInvalidKjoeretid(String kjoeretid) throws IOException {
        String path = createTestFolders();

        exception.expect(InvalidParameterException.class);
        exception.expectMessage("kjoeretid");

        new TidsserieArgumentsFactory().create("-b", "test", "-o", path, "-i", path, "-kjoeretid", kjoeretid);
    }

    @Test
    public void testInvalidSluttidWithlLettersThrowsException() throws Exception {
        testInvalidSluttidspunkt("a");
    }

    private void testInvalidSluttidspunkt(String sluttid) throws IOException {
        String path = createTestFolders();

        exception.expect(InvalidParameterException.class);
        exception.expectMessage("sluttid");

        new TidsserieArgumentsFactory().create("-b", "test", "-o", path, "-i", path, "-sluttid", sluttid);
    }

    @Test
    public void testArgsMedBeskrivelseOgHjelpThrowsUsageRequestedException() throws Exception {
        exception.expect(UsageRequestedException.class);

        new TidsserieArgumentsFactory().create("-help");
    }

    private String createTestFolders() throws IOException {
        String path = testFolder.newFolder().getAbsolutePath();
        Paths.get(path, "grunnlagsdata_2015-01-01_01-01-01-01").toFile().mkdir();
        return path;
    }


    @Test
    public void testMissingBatchIdThrowsException() throws Exception {
        File file = testFolder.newFolder(name.getMethodName());

        exception.expect(InvalidParameterException.class);
        exception.expectMessage("Det finnes ingen batch-kataloger i ");

        new TidsserieArgumentsFactory().create(
                "-b", "Test batch id missing",
                "-i", file.getAbsolutePath(),
                "-o", file.getAbsolutePath(),
                "-log", file.getAbsolutePath(),
                "-m", defaultModus
        );
    }

    @Test
    public void testMissingBatchIdSelectsNewestBatchFolder() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        String expectedBatchFolder = "grunnlagsdata_2015-01-01_01-00-00-01";
        path.resolve("grunnlagsdata_2015-01-01_01-00-00-00").toFile().mkdir();
        path.resolve(expectedBatchFolder).toFile().mkdir();

        ProgramArguments programArguments = new TidsserieArgumentsFactory().create(
                "-b", "Test set default batch id",
                "-i", path.toAbsolutePath().toString(),
                "-log", path.toAbsolutePath().toString(),
                "-o", path.toAbsolutePath().toString(),
                "-m", defaultModus
        );

        assertThat(programArguments.getGrunnlagsdataBatchId()).isEqualTo(expectedBatchFolder);

    }


}