package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;


import static no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator.ChecksumValideringAvGrunnlagsdata.MD5_CHECKSUMS_FILENAME;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ChecksumValideringAvGrunnlagsdataTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

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
        exception.expect(UgyldigUttrekkException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME + " mangler i katalogen");

        validate();
    }

    @Test
    public void testEmptyChecksumfileThrowsException() throws IOException {
        exception.expect(UgyldigUttrekkException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME + " er tom.");

        Files.createFile(md5sums.toPath());

        validate();
    }

    @Test
    public void testCorruptChecksumfileThrowsException() {
        exception.expect(UgyldigUttrekkException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME + " er korrupt.");

        writeMD5("invalid md5");

        validate();
    }

    @Test
    public void testMissingFileThrowsException() {
        exception.expect(UgyldigUttrekkException.class);
        exception.expectMessage("Følgende filer er oppført i " + MD5_CHECKSUMS_FILENAME + " men finnes ikke i ");
        writeMD5("123 *123.txt");

        validate();
    }


    @Test
    public void testInvalidChecksumThrowsException() {
        exception.expect(UgyldigUttrekkException.class);
        exception.expectMessage("Følgende filer har en annen m5d-sjekksum enn oppgitt");

        write("some;checksumFile;content", fil("test.csv"));
        writeMD5("deadbeef *test.csv");

        validate();
    }

    @Test
    public void testHappyday() {
        write("some;checksumFile;content", fil("test.csv"));
        writeMD5("e0b562ed7f852b4f8a887c61484a9255 *test.csv");

        validate();
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