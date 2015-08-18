package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.StandardOutputAndError;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

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

    @Rule
    public final StandardOutputAndError console = new StandardOutputAndError();

    @Test
    public void testDeletePreviousBatches() throws Exception {
        Path path = testFolder.newFolder(name.getMethodName()).toPath();

        String doNotDeleteFilename = "doNotDelete.txt";
        String tidsserieIgnored = "tidsserieIgnored";

        //skal ikke slettes
        path.resolve(tidsserieIgnored).toFile().mkdir();
        path.resolve(doNotDeleteFilename).toFile().createNewFile();

        //slettes
        path.resolve("tidsserie").toFile().mkdir();
        path.resolve("tidsserie_2015-01-01_01-00-00-00").toFile().mkdir();
        path.resolve("tidsserie_2015-01-01_01-00-00-01").toFile().mkdir();

        try (final Stream<Path> list = Files.list(path)) {
            assertThat(list.count()).isEqualTo(5);
        }

        new BatchDirectoryCleaner(new BatchId(LocalDateTime.now()), path).deleteAllPreviousBatches();

        try (final Stream<Path> stream = Files.list(path)) {
            List<String> list = stream.map(p -> p.toFile().getName()).collect(toList());
            assertThat(list).containsExactly(doNotDeleteFilename, tidsserieIgnored);
        }
    }
}