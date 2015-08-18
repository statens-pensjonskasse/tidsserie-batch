package no.spk.pensjon.faktura.tidsserie.batch.main;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class MetaDataWriterTest {
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

//    @Rule
//    public final Log4j log4j = new Log4j().failOn(Level.ERROR, Level.WARN);

    @Test
    public void testCreateMetadataFile() throws Exception {
        File writeFolder = createTestFolders();

        MetaDataWriter metaDataWriter = getMetaDataWriter(writeFolder);
        BatchId batchId = new BatchId(LocalDateTime.now());

        ProgramArguments programArguments = getProgramArguments(writeFolder);
        Optional<File> metadataFile = metaDataWriter.createMetadataFile(programArguments, batchId, Duration.of(10, ChronoUnit.MILLIS));
        assertThat(metadataFile.isPresent()).isTrue();
        assertThat(metadataFile.get()).exists();

        String fileContent = new String(Files.readAllBytes(metadataFile.get().toPath()), Charset.forName("cp1252"));
        assertThat(fileContent).contains("Batch-id: " + batchId);
    }

    @Test
    public void testCreateChecksumFile() throws Exception {
        File writeFolder = createTestFolders();

        MetaDataWriter metaDataWriter = getMetaDataWriter(writeFolder);

        String content = "someContent";
        String memoryHex = DigestUtils.md5Hex(content);
        Files.write(Paths.get(writeFolder.getAbsolutePath(), "some.csv"), content.getBytes());

        Optional<File> checksumFile = metaDataWriter.createChecksumFile();
        assertThat(checksumFile.isPresent()).isTrue();
        assertThat(checksumFile.get()).exists();

        String fileContent = new String(Files.readAllBytes(checksumFile.get().toPath()), Charset.forName("cp1252"));

        String[] split = fileContent.trim().split(" ");
        assertThat(split.length).isEqualTo(2);

        String fileHex = split[0];
        String fileName = split[1];
        assertThat(fileHex).isEqualTo(memoryHex);
        assertThat(fileName).isEqualTo("*some.csv");
    }

    private ProgramArguments getProgramArguments(File writeFolder) {
        String file = writeFolder.getAbsolutePath();
        return ProgramArgumentsFactory.create("-b", "lager metadata", "-i", file, "-o", file, "-log", file);
    }

    private MetaDataWriter getMetaDataWriter(File writeFolder) {
        return new MetaDataWriter(TemplateConfigurationFactory.create(), writeFolder.toPath());
    }

    private File createTestFolders() throws IOException {
        File writeFolder = testFolder.newFolder(name.getMethodName());
        Paths.get(writeFolder.getAbsolutePath(), "grunnlagsdata_2015-01-01_01-01-01-01").toFile().mkdir();
        return writeFolder;
    }

}