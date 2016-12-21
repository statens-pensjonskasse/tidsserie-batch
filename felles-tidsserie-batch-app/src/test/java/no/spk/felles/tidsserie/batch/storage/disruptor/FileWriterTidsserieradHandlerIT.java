package no.spk.felles.tidsserie.batch.storage.disruptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import no.spk.felles.tidsserie.batch.TemporaryFolderWithDeleteVerification;
import no.spk.felles.tidsserie.batch.core.lagring.Tidsserierad;

import org.assertj.core.api.AbstractListAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Integrasjonstestar for {@link FileWriterTidsserieradHandler}.
 *
 * @author Tarjei Skorgenes
 */
public class FileWriterTidsserieradHandlerIT {
    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private FileTemplate template;

    private FileWriterTidsserieradHandler consumer;

    @Before
    public void _before() throws IOException {
        when(template.createUniqueFile(anyInt(), anyString())).thenAnswer(invocation -> temp.newFile());
        consumer = new FileWriterTidsserieradHandler(template);
    }

    @After
    public void _after() throws IOException {
        consumer.close();
    }

    /**
     * Dersom input-fila mot formodning eksisterer frå før, skal consumeren trunkere den slik
     * at resultata av tidligare køyringar ikkje blir blanda med resultata frå neste køyring.
     */
    @Test
    public void skalTrunkereOutputfilaVissDenEksistererFraFoer() throws IOException {
        final File alreadyExists = temp.newFile();
        Files.write(alreadyExists.toPath(), "YADA YADA\n".getBytes());
        when(template.createUniqueFile(anyInt(), anyString())).thenReturn(alreadyExists);

        final Tidsserierad event = new Tidsserierad();
        event.buffer.append("MOAR MOAR MOAR\n");
        consumer.onEvent(event, 1, true);

        assertFileContent(alreadyExists)
                .hasSize(1)
                .containsOnly("MOAR MOAR MOAR");
    }

    protected AbstractListAssert<?, ? extends List<? extends String>, String> assertFileContent(
            final File file) throws IOException {
        return assertThat(Files.readAllLines(file.toPath()))
                .as("alle linjer i " + file);
    }

    @Test
    public void skalSkriveEventenTilForskjelligeFilerBasertPaaEventserien() throws IOException {
        when(template.createUniqueFile(1L, "tidsserie")).thenAnswer(a -> temp.newFile("1"));
        when(template.createUniqueFile(2L, "tidsserie")).thenAnswer(a -> temp.newFile("2"));

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(2L).medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);

        assertFileContent(new File(temp.getRoot(), "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp.getRoot(), "2")).hasSize(1).containsOnly("YAY");
    }

    @Test
    public void skalSkriveEventenTilForskjelligeFilerBasertPaaPrefix() throws IOException {
        when(template.createUniqueFile(1L, "tidsserie")).thenAnswer(a -> temp.newFile("1"));
        when(template.createUniqueFile(1L, "noeAnnet")).thenAnswer(a -> temp.newFile("2"));

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(1L).medFilprefix("noeAnnet").medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);

        assertFileContent(new File(temp.getRoot(), "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp.getRoot(), "2")).hasSize(1).containsOnly("YAY");
    }
}