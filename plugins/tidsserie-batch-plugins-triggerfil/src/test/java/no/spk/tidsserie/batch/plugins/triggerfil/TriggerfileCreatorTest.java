package no.spk.tidsserie.batch.plugins.triggerfil;

import static no.spk.tidsserie.batch.core.TidsserieGenerertCallback2.metadata;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import no.spk.tidsserie.input.BatchId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TriggerfileCreatorTest {

    private Path utKatalog;

    private TriggerfileCreator creator;

    @BeforeEach
    void _before(@TempDir Path temp) {
        utKatalog = temp;
        creator = new TriggerfileCreator(utKatalog);
    }

    @Test
    void createTriggerFile() {
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