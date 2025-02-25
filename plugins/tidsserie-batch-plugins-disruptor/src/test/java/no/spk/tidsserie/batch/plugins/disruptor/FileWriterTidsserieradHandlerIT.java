package no.spk.tidsserie.batch.plugins.disruptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.will;
import static org.mockito.BDDMockito.willReturn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import no.spk.tidsserie.batch.core.lagring.Tidsserierad;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integrasjonstestar for {@link FileWriterTidsserieradHandler}.
 *
 * @author Tarjei Skorgenes
 */
@ExtendWith(MockitoExtension.class)
public class FileWriterTidsserieradHandlerIT {

    @Mock
    private FileTemplate template;

    private FileWriterTidsserieradHandler consumer;

    @BeforeEach
    void _before() {
        consumer = new FileWriterTidsserieradHandler(template);
    }

    @AfterEach
    void _after() throws IOException {
        consumer.close();
    }

    /**
     * Dersom input-fila mot formodning eksisterer frå før, skal consumeren trunkere den slik
     * at resultata av tidligare køyringar ikkje blir blanda med resultata frå neste køyring.
     */
    @Test
    void skalTrunkereOutputfilaVissDenEksistererFraFoer(@TempDir File temp) throws IOException {
        final File alreadyExists = new File(temp, "junit");
        Files.write(alreadyExists.toPath(), "YADA YADA\n".getBytes());
        willReturn(alreadyExists).given(template).createUniqueFile(1L, "tidsserie");

        final Tidsserierad event = new Tidsserierad();
        event.buffer.append("MOAR MOAR MOAR\n");
        consumer.onEvent(event, 1, true);


        assertFileContent(alreadyExists)
                .hasSize(1)
                .containsOnly("MOAR MOAR MOAR");
    }

    @Test
    void skalSkriveEventenTilForskjelligeFilerBasertPaaEventserien(@TempDir File temp) throws IOException {
        will(a -> new File(temp,"1")).given(template).createUniqueFile(1L, "tidsserie");
        will(a -> new File(temp,"2")).given(template).createUniqueFile(2L, "tidsserie");

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(2L).medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);

        assertFileContent(new File(temp, "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp, "2")).hasSize(1).containsOnly("YAY");
    }

    @Test
    void skalSkriveEventenTilForskjelligeFilerBasertPaaPrefix(@TempDir File temp) throws IOException {
        will(a -> new File(temp, "1")).given(template).createUniqueFile(1L, "tidsserie");
        will(a -> new File(temp, "2")).given(template).createUniqueFile(1L, "noeAnnet");

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(1L).medFilprefix("noeAnnet").medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);

        assertFileContent(new File(temp, "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp, "2")).hasSize(1).containsOnly("YAY");
    }

    private ListAssert<String> assertFileContent(
            final File file) throws IOException {
        return assertThat(Files.readAllLines(file.toPath()))
                .as("alle linjer i " + file);
    }
}