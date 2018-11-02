package no.spk.felles.tidsserie.batch.plugins.triggerfil;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import no.spk.pensjon.faktura.tjenesteregister.support.SimpleServiceRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TriggerfileCreatorTest {
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    @Test
    public void testCreateTriggerFile() throws Exception {
        Path writeFolder = testFolder.newFolder(name.getMethodName()).toPath();
        new TriggerfileCreator(writeFolder).tidsserieGenerert(new SimpleServiceRegistry());
        assertThat(writeFolder.resolve("ok.trg")).isRegularFile();
    }
}