package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Integrasjonstestar for {@link FileWriterObservasjonsConsumer}.
 *
 * @author Tarjei Skorgenes
 */
public class FileWriterObservasjonsConsumerIT {
    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private FileTemplate template;

    private FileWriterObservasjonsConsumer consumer;

    @Before
    public void _before() throws IOException {
        when(template.createUniqueFile()).thenAnswer(invocation -> temp.newFile());
        consumer = new FileWriterObservasjonsConsumer(template);
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
        when(template.createUniqueFile()).thenReturn(alreadyExists);

        try (final FileWriter writer = consumer.newWriter(template)) {
            writer.write("MOAR MOAR MOAR\n");
        }

        assertThat(Files.readAllLines(alreadyExists.toPath()))
                .hasSize(1)
                .containsOnly("MOAR MOAR MOAR");
    }
}