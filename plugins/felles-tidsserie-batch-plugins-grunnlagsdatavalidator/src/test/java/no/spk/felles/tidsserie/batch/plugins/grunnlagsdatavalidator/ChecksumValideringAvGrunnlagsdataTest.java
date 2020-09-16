package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;


import static no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator.ChecksumValideringAvGrunnlagsdata.MD5_CHECKSUMS_FILENAME;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ChecksumValideringAvGrunnlagsdataTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    private ChecksumValideringAvGrunnlagsdata validator;
    private File uttrekkskatalog;
    private File md5sums;

    @Before
    public void _before() throws IOException {
        uttrekkskatalog = testFolder.newFolder(name.getMethodName());
        md5sums = fil(MD5_CHECKSUMS_FILENAME);
        validator = new ChecksumValideringAvGrunnlagsdata(uttrekkskatalog.toPath());
    }

    @Test
    public void testMissingChecksumfileThrowsException() {
        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " mangler i katalogen");
    }

    @Test
    public void testEmptyChecksumfileThrowsException() throws IOException {
        Files.createFile(md5sums.toPath());

        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " er tom.");
    }

    @Test
    public void testCorruptChecksumfileThrowsException() {
        writeMD5("invalid md5");

        assertValidate().hasMessageContaining(MD5_CHECKSUMS_FILENAME + " er korrupt.");
    }

    @Test
    public void testMissingFileThrowsException() {
        writeMD5("123 *123.txt");

        assertValidate().hasMessageContaining("Følgende filer er oppført i " + MD5_CHECKSUMS_FILENAME + " men finnes ikke i ");
    }


    @Test
    public void testInvalidChecksumThrowsException() {
        write("some;checksumFile;content", fil("test.csv"));
        writeMD5("deadbeef *test.csv");

        assertValidate().hasMessageContaining("Følgende filer har en annen m5d-sjekksum enn oppgitt");
    }

    @Test
    public void testHappyday() {
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
}