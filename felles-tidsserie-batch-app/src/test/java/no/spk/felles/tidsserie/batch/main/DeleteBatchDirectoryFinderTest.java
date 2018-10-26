package no.spk.felles.tidsserie.batch.main;

import static no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import no.spk.felles.tidsserie.batch.TemporaryFolderWithDeleteVerification;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;

import org.assertj.core.api.ObjectArrayAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DeleteBatchDirectoryFinderTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    private Path logKatalog;

    private Path utKatalog;
    private DeleteBatchDirectoryFinder finder;

    @Before
    public void _before() throws IOException {
        logKatalog = testFolder.newFolder("log").toPath();
        utKatalog = testFolder.newFolder("ut").toPath();
        finder = new DeleteBatchDirectoryFinder(utKatalog, logKatalog);
    }

    @SuppressWarnings("unused")
    @Test
    public void skal_kun_inkludere_logkatalog_eldre_enn_aldersgrensa_og_utkatalog() throws Exception {
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
}