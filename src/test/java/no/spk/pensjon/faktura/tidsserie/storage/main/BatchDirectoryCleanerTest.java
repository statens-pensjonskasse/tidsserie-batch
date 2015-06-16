package no.spk.pensjon.faktura.tidsserie.storage.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.time.LocalDateTime;

import no.spk.pensjon.faktura.tidsserie.TemporaryFolderWithDeleteVerification;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class BatchDirectoryCleanerTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Test
    public void testDeletePreviousBatches() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        String doNotDeleteFilename = "doNotDelete.txt";
        path.resolve("tidsserie_2015-01-01_01-00-00-00").toFile().mkdir();
        path.resolve("tidsserie_2015-01-01_01-00-00-01").toFile().mkdir();
        path.resolve(doNotDeleteFilename).toFile().createNewFile();

        try (final Stream<Path> list = Files.list(path)) {
            assertThat(list.count()).isEqualTo(3);
        }

        new BatchDirectoryCleaner(path, new BatchId(LocalDateTime.now())).deleteAllPreviousBatches();

        try (final Stream<Path> list = Files.list(path)) {
            assertThat(list.count()).isEqualTo(1);
        }
        try (final Stream<Path> list = Files.list(path)) {
            assertThat(list.findFirst().get().toFile().getName()).isEqualTo(doNotDeleteFilename);
        }
    }
}