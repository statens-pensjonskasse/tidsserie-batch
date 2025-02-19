package no.spk.premie.tidsserie.batch.plugins.metadatawriter;

import static no.spk.premie.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import no.spk.faktura.input.BatchId;
import no.spk.premie.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.premie.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.premie.tidsserie.batch.main.input.ProgramArguments;
import no.spk.premie.tidsserie.batch.main.input.TidsserieArgumentsFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class MetaDataWriterTest {


    @RegisterExtension
    public final ModusExtension modus = new ModusExtension();

    private final TidsserieArgumentsFactory parser = new TidsserieArgumentsFactory();

    @Test
    public void testCreateMetadataFile(@TempDir File temp) throws Exception {
        final File writeFolder = newFolder(temp, "inn");
        newFolder(writeFolder, "grunnlagsdata_2015-01-01_01-01-01-01");

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

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}