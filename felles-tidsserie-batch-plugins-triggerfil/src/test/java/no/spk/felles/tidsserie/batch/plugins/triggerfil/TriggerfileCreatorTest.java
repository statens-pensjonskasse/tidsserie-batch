package no.spk.felles.tidsserie.batch.plugins.triggerfil;

import static no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback.metadata;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import no.spk.faktura.input.BatchId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TriggerfileCreatorTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private Path utKatalog;

    private TriggerfileCreator creator;

    @Before
    public void _before() {
        utKatalog = temp.getRoot().toPath();
        creator = new TriggerfileCreator(utKatalog);
    }

    @Test
    public void testCreateTriggerFile() {
        generer();

        assertThat(utKatalog.resolve("ok.trg"))
                .exists()
                .isRegularFile();
    }

    private void generer() {
        creator.tidsserieGenerert(null,
                metadata(
                        new BatchId("1234", LocalDateTime.MIN),
                        Duration.ZERO
                )
        );
    }
}