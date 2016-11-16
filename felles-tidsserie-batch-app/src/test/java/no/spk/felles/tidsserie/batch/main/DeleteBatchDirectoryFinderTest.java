package no.spk.felles.tidsserie.batch.main;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import no.spk.felles.tidsserie.batch.TemporaryFolderWithDeleteVerification;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DeleteBatchDirectoryFinderTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Test
    public void testFindDataDirectoryAndOldBatches() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        Path logDir = path.resolve("logDir");
        Path dataDir = path.resolve("dataDir");

        Path deleteOld1 = logDir.resolve("tidsserie_2013-01-01_01-00-00-00");
        Path deleteOld2 = logDir.resolve("tidsserie_2015-01-01_01-00-00-00");
        Path doNotDelete1 = logDir.resolve("tidsserie_2099-01-01_01-00-00-00");
        Path doNotDelete2 = logDir.resolve("unrelatedFolder");

        asList(logDir, dataDir, deleteOld1, deleteOld2, doNotDelete1, doNotDelete2)
                .stream()
                .map(s -> dataDir.resolve(s).toFile())
                .forEach(File::mkdir);

        DeleteBatchDirectoryFinder finder = new DeleteBatchDirectoryFinder(dataDir, logDir);
        Path[] deletable = finder.findDeletableBatchDirectories(1);

        assertThat(deletable).containsOnly(dataDir, deleteOld1, deleteOld2);
    }
}