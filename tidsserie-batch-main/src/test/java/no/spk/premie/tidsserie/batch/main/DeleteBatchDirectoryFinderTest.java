package no.spk.premie.tidsserie.batch.main;

import static no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;

import org.assertj.core.api.ObjectArrayAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DeleteBatchDirectoryFinderTest {

    @TempDir
    public File testFolder;

    private Path logKatalog;

    private Path utKatalog;
    private DeleteBatchDirectoryFinder finder;

    @BeforeEach
    void _before() throws IOException {
        logKatalog = newFolder(testFolder, "log").toPath();
        utKatalog = newFolder(testFolder, "ut").toPath();
        finder = new DeleteBatchDirectoryFinder(utKatalog, logKatalog);
    }

    @SuppressWarnings("unused")
    @Test
    void skal_kun_inkludere_logkatalog_eldre_enn_aldersgrensa_og_utkatalog() throws Exception {
        Path deleteOld1 = logkatalog("tidsserie_2013-01-01_01-00-00-00");
        Path deleteOld2 = logkatalog("tidsserie_2015-01-01_01-00-00-00");
        Path doNotDelete1 = logkatalog("tidsserie_2099-01-01_01-00-00-00");
        Path doNotDelete2 = logkatalog("unrelatedFolder");

        assertFind(
                aldersgrenseForSlettingAvLogKatalogar(1)
        )
                .containsOnly(utKatalog, deleteOld1, deleteOld2);
    }

    private ObjectArrayAssert<Path> assertFind(final AldersgrenseForSlettingAvLogKatalogar aldersgrense) throws HousekeepingException {
        return assertThat(
                finder.findDeletableBatchDirectories(aldersgrense)
        );
    }

    private Path logkatalog(final String katalognavn) {
        final Path underkatalog = this.logKatalog.resolve(katalognavn);
        assertThat(underkatalog.toFile().mkdirs()).isTrue();
        return underkatalog;
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}