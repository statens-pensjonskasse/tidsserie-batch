package no.spk.premie.tidsserie.batch.plugins.grunnlagsdatavalidator;


import static no.spk.premie.tidsserie.batch.plugins.grunnlagsdatavalidator.ChecksumValideringAvGrunnlagsdata.MD5_CHECKSUMS_FILENAME;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ChecksumValideringAvGrunnlagsdataTest {

    @TempDir
    public File testFolder;

    
    public String name;

    private ChecksumValideringAvGrunnlagsdata validator;
    private File uttrekkskatalog;
    private File md5sums;

    @BeforeEach
    void _before(TestInfo testInfo) throws IOException {
        Optional<Method> testMethod = testInfo.getTestMethod();
        testMethod.ifPresentOrElse(method -> this.name = method.getName(), () -> this.name = "unknown");
        uttrekkskatalog = newFolder(testFolder, name);
        md5sums = fil(MD5_CHECKSUMS_FILENAME);
        validator = new ChecksumValideringAvGrunnlagsdata(uttrekkskatalog.toPath());
    }

    @Test
    void missingChecksumfileThrowsException() {
        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " mangler i katalogen");
    }

    @Test
    void emptyChecksumfileThrowsException() throws IOException {
        Files.createFile(md5sums.toPath());

        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " er tom.");
    }

    @Test
    void corruptChecksumfileThrowsException() {
        writeMD5("invalid md5");

        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " er korrupt.");
    }

    @Test
    void missingFileThrowsException() {
        writeMD5("123 *123.txt");

        assertValidate().hasMessageContaining("Følgende filer er oppført i " + MD5_CHECKSUMS_FILENAME + " men finnes ikke i ");
    }


    @Test
    void invalidChecksumThrowsException() {
        write("some;checksumFile;content", fil("test.csv"));
        writeMD5("deadbeef *test.csv");

        assertValidate().hasMessageContaining("Følgende filer har en annen m5d-sjekksum enn oppgitt");
    }

    @Test
    void happyday() {
        write("some;checksumFile;content", fil("test.csv"));
        writeMD5("e0b562ed7f852b4f8a887c61484a9255 *test.csv");

        validate();
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertValidate() {
        return assertThatCode(this::validate).isInstanceOf(UgyldigUttrekkException.class);
    }

    private void writeMD5(final String linje) {
        write(linje, md5sums);
    }

    private void validate() {
        validator.validate();
    }

    private File fil(final String fil) {
        return uttrekkskatalog.toPath().resolve(fil).toFile();
    }

    private void write(final String content, final File file) {
        try {
            assert file.createNewFile();
            if (!content.isEmpty()) {
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
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