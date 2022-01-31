package no.spk.felles.tidsserie.batch.plugins.disruptor.gzipped;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.will;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import no.spk.felles.tidsserie.batch.core.lagring.Tidsserierad;

import org.assertj.core.api.ListAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

/**
 * Integrasjonstestar for {@link GzipWriterTidsserieradHandler}.
 *
 */
public class GzipWriterTidsserieradHandlerIT {
    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final MockitoRule mockito = rule().strictness(STRICT_STUBS);

    @Mock
    private FileTemplate template;

    private GzipWriterTidsserieradHandler consumer;

    @Before
    public void _before() {
        consumer = new GzipWriterTidsserieradHandler(template);
    }

    @After
    public void _after() throws IOException {
        consumer.close();
    }

    /**
     * Dersom input-fila mot formodning eksisterer frå før, skal consumeren trunkere den slik at resultata av tidligare køyringar ikkje blir blanda med resultata frå neste køyring.
     */
    @Test
    public void skalTrunkereOutputfilaVissDenEksistererFraFoer() throws IOException {
        final File alreadyExists = temp.newFile();
        Files.write(alreadyExists.toPath(), "YADA YADA\n".getBytes());
        willReturn(alreadyExists).given(template).createUniqueFile(1L, "tidsserie");

        final Tidsserierad event = new Tidsserierad();
        event.buffer.append("MOAR MOAR MOAR\n");
        consumer.onEvent(event, 1, true);
        consumer.close();

        assertFileContent(alreadyExists)
                .hasSize(1)
                .containsOnly("MOAR MOAR MOAR");
    }

    @Test
    public void skalSkriveEventenTilForskjelligeFilerBasertPaaEventserien() throws IOException {
        will(a -> temp.newFile("1")).given(template).createUniqueFile(1L, "tidsserie");
        will(a -> temp.newFile("2")).given(template).createUniqueFile(2L, "tidsserie");

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(2L).medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);
        consumer.close();

        assertFileContent(new File(temp.getRoot(), "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp.getRoot(), "2")).hasSize(1).containsOnly("YAY");
    }

    @Test
    public void skalSkriveEventenTilForskjelligeFilerBasertPaaPrefix() throws IOException {
        will(a -> temp.newFile("1")).given(template).createUniqueFile(1L, "tidsserie");
        will(a -> temp.newFile("2")).given(template).createUniqueFile(1L, "noeAnnet");

        final Tidsserierad event = new Tidsserierad();

        event.serienummer(1L).medInnhold("YEY\n");
        consumer.onEvent(event, 1, true);

        event.serienummer(1L).medFilprefix("noeAnnet").medInnhold("YAY\n");
        consumer.onEvent(event, 1, true);
        consumer.close();

        assertFileContent(new File(temp.getRoot(), "1")).hasSize(1).containsOnly("YEY");
        assertFileContent(new File(temp.getRoot(), "2")).hasSize(1).containsOnly("YAY");
    }

    private ListAssert<String> assertFileContent(final File file) throws IOException {
        return assertThat(
                decompressGzip(file)
        )
                .as("alle linjer i " + file);
    }

    List<String> decompressGzip(final File source) throws IOException {
        final FileInputStream fin = new FileInputStream(source);
        final GZIPInputStream gzis = new GZIPInputStream(fin);
        final InputStreamReader xover = new InputStreamReader(gzis);
        final BufferedReader is = new BufferedReader(xover);

        String line;
        final List<String> result = new ArrayList<>();
        while ((line = is.readLine()) != null) {
            result.add(line);
        }

        return result;
    }
}