package no.spk.felles.tidsserie.batch.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(StandardOutputAndError.class)
public class DirectoryCleanerTest {

    @Test
    void deleteDirectories(@TempDir Path path) throws Exception {

        //Path path = newFolder(testFolder, name).toPath();

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
            List<String> list = stream.map(p -> p.toFile().getName()).toList();
            assertThat(list).isEmpty();
        }
    }

    @Test
    void deleteMissingDirectories(@TempDir Path path) throws Exception {

        Path path1 = path.resolve("tidsserie");
        Path path2 = path.resolve("tidsserie_2015-01-01_01-00-00-00");

        new DirectoryCleaner(path1, path2).deleteDirectories();
    }

}
