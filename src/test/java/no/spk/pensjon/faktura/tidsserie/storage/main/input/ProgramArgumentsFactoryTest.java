package no.spk.pensjon.faktura.tidsserie.storage.main.input;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.InvalidParameterException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ProgramArgumentsFactoryTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final TestName name = new TestName();


    @Test
    public void testMissingBatchIdThrowsException() throws Exception {
        File file = testFolder.newFolder(name.getMethodName());

        exception.expect(InvalidParameterException.class);
        exception.expectMessage("Det finnes ingen batch-kataloger i ");

        ProgramArgumentsFactory.create("-b", "Test batch id missing",
                                        "-inn", file.getAbsolutePath());
    }

    @Test
    public void testMissingBatchIdSelectsNewestBatchFolder() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        String expectedBatchFolder = "grunnlagsdata_2015-01-01_01-00-00-01";
        path.resolve("grunnlagsdata_2015-01-01_01-00-00-00").toFile().mkdir();
        path.resolve(expectedBatchFolder).toFile().mkdir();

        ProgramArguments programArguments = ProgramArgumentsFactory.create("-b", "Test set default batch id",
                "-inn", path.toAbsolutePath().toString());

        assertThat(programArguments.getGrunnlagsdataBatchId()).isEqualTo(expectedBatchFolder);

    }
}