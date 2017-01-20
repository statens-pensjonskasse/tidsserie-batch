package no.spk.felles.tidsserie.batch.main;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.TemporaryFolderWithDeleteVerification;
import no.spk.felles.tidsserie.batch.main.input.StandardOutputAndError;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DirectoryCleanerTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final StandardOutputAndError console = new StandardOutputAndError();

    @Test
      public void testDeleteDirectories() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        //slettes
        Path path1 = path.resolve("tidsserie");
        path1.toFile().mkdir();
        Path path2 = path.resolve("tidsserie_2015-01-01_01-00-00-00");
        path2.toFile().mkdir();
        path2.resolve("somefile.txt").toFile().createNewFile();

        try (final Stream<Path> list = Files.list(path)) {
            assertThat(list.count()).isEqualTo(2);
        }

        new DirectoryCleaner(path1, path2).deleteDirectories();

        try (final Stream<Path> stream = Files.list(path)) {
            List<String> list = stream.map(p -> p.toFile().getName()).collect(toList());
            assertThat(list).isEmpty();
        }
    }

    @Test
    public void testDeleteMissingDirectories() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        Path path1 = path.resolve("tidsserie");
        Path path2 = path.resolve("tidsserie_2015-01-01_01-00-00-00");

        new DirectoryCleaner(path1, path2).deleteDirectories();
    }
}