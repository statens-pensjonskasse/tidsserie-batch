package no.spk.felles.tidsserie.batch.plugins.metadatawriter;

import static no.spk.felles.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import no.spk.faktura.input.BatchId;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;
import no.spk.felles.tidsserie.batch.main.input.TidsserieArgumentsFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class MetaDataWriterTest {
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final ModusRule modus = new ModusRule();

    private final TidsserieArgumentsFactory parser = new TidsserieArgumentsFactory();

    @Test
    public void testCreateMetadataFile() throws Exception {
        final File writeFolder = createTestFolders();

        final MetaDataWriter metaDataWriter = getMetaDataWriter(writeFolder);
        final BatchId batchId = new BatchId(TIDSSERIE_PREFIX, LocalDateTime.now());

        final ProgramArguments programArguments = getProgramArguments(writeFolder);
        final Optional<File> metadataFile = metaDataWriter.createMetadataFile(programArguments, batchId, Duration.of(10, ChronoUnit.MILLIS));
        assertThat(metadataFile.isPresent()).isTrue();
        assertThat(metadataFile.get()).exists();

        final String fileContent = new String(Files.readAllBytes(metadataFile.get().toPath()), StandardCharsets.UTF_8);
        assertThat(fileContent).contains("Batch-id: " + batchId);
    }

    private ProgramArguments getProgramArguments(final File writeFolder) throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException {
        final String navn = "modus";
        this.modus.support(navn);
        final String file = writeFolder.getAbsolutePath();
        return (ProgramArguments) parser.parse(
                "-b", "lager metadata",
                "-i", file,
                "-o", file,
                "-log", file,
                "-m", navn
        );
    }

    private MetaDataWriter getMetaDataWriter(final File writeFolder) {
        return new MetaDataWriter(TemplateConfigurationFactory.create(), writeFolder.toPath());
    }

    private File createTestFolders() throws IOException {
        final File writeFolder = testFolder.newFolder(name.getMethodName());
        Paths.get(writeFolder.getAbsolutePath(), "grunnlagsdata_2015-01-01_01-01-01-01").toFile().mkdir();
        return writeFolder;
    }
}