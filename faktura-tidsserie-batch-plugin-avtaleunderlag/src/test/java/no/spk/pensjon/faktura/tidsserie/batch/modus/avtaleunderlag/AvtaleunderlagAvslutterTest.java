package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagAvslutterTest {
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testCreateCsvGroupFilesFor271CsvFiles() throws Exception {
        testCreateCsvGroupFiles(271);
    }

    @Test
    public void testCreateCsvGroupFilesFor1CsvFiles() throws Exception {
        testCreateCsvGroupFiles(1);
    }

    @Test
    public void testCreateCsvGroupFilesFor0CsvFiles() throws Exception {
        testCreateCsvGroupFiles(0);
    }

    private void testCreateCsvGroupFiles(int numberOfCsvFiles) throws IOException {
        File writeFolder = testFolder.newFolder(name.getMethodName());
        for (int i = 0; i < numberOfCsvFiles; i++) {
            writeFolder.toPath().resolve("tidsserie" + i + ".csv").toFile().createNewFile();
        }

       new AvtaleunderlagAvslutter(writeFolder.toPath())
               .lagCsvGruppefiler();

        Pattern groupPattern = Pattern.compile("^FFF_FILLISTE_(\\d)+.txt$");
        File[] groupFiles = writeFolder.listFiles(f -> groupPattern.matcher(f.getName()).matches());
        assertThat(groupFiles).hasSize(10);

        Pattern csvPattern = Pattern.compile("^tidsserie.*.csv");
        File[] csvFiles = writeFolder.listFiles(f -> csvPattern.matcher(f.getName()).matches());
        assertThat(csvFiles.length).isGreaterThanOrEqualTo(10);

        stream(groupFiles).forEach(f -> assertAllLinesMatch(f.toPath(), CsvFileGroupWriter.CSV_PATTERN));
    }

    private void assertAllLinesMatch(Path file, Pattern linePattern){
        try {
            List<String> lines = Files.readAllLines(file);
            assertThat(lines).isNotEmpty();
            lines.stream().forEach(s -> assertThat(s).matches(linePattern));
        } catch (IOException e) {
            fail("Kunne ikke lese fil.", e);
        }
    }

    private File createTestFolders() throws IOException {
        File writeFolder = testFolder.newFolder(name.getMethodName());
        Paths.get(writeFolder.getAbsolutePath(), "grunnlagsdata_2015-01-01_01-01-01-01").toFile().mkdir();
        return writeFolder;
    }
}