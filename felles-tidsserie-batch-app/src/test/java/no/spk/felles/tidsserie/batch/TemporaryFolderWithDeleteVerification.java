package no.spk.felles.tidsserie.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Utvidelse av {@link TemporaryFolder} som feilar dersom opprydding/sletting feilar på ei eller fleire
 * av dei temporære filene/katalogane som regelen har oppretta.
 *
 * @author Tarjei Skorgenes
 */
public class TemporaryFolderWithDeleteVerification extends TemporaryFolder {
    /**
     * Delete all files and folders under the temporary folder. Usually not
     * called directly, since it is automatically applied by the {@link Rule}
     */
    public void delete() {
        final File folder = getRoot();
        final Set<File> deleteFailures = new HashSet<>();
        if (folder != null) {
            recursiveDelete(deleteFailures, folder);
        }
        assertThat(deleteFailures)
                .as("filer/katalogar som ikkje det var mulig å slette")
                .isEmpty();
    }

    private void recursiveDelete(final Set<File> deleteFailures, final File file) {
        final File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(deleteFailures, each);
            }
        }
        if (!file.delete()) {
            deleteFailures.add(file);
        }
    }
}
