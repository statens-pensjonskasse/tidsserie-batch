package no.spk.pensjon.faktura.tidsserie.batch.main;


import static no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataDirectoryValidator.MD5_CHECKSUMS_FILENAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * @author Snorre E. Brekke - Computas
 */
public class GrunnlagsdataDirectoryValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    @Test
    public void testMissingChecksumfileThrowsException() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        exception.expect(GrunnlagsdataException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME + " mangler i katalogen");

        validator.validate();
    }

    @Test
    public void testEmptyChecksumfileThrowsException() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());
        grunnlagsdataBatchKatalog.toPath().resolve(MD5_CHECKSUMS_FILENAME).toFile().createNewFile();

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        exception.expect(GrunnlagsdataException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME +" er tom.");

        validator.validate();
    }

    @Test
    public void testCorruptChecksumfileThrowsException() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());
        File file = grunnlagsdataBatchKatalog.toPath().resolve(MD5_CHECKSUMS_FILENAME).toFile();
        file.createNewFile();

        write("invalid md5", file);

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        exception.expect(GrunnlagsdataException.class);
        exception.expectMessage(MD5_CHECKSUMS_FILENAME +" er korrupt.");

        validator.validate();
    }

    @Test
    public void testMissingFileThrowsException() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());
        File checksumFile = grunnlagsdataBatchKatalog.toPath().resolve(MD5_CHECKSUMS_FILENAME).toFile();
        checksumFile.createNewFile();
        write("123 *123.txt", checksumFile);

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        exception.expect(GrunnlagsdataException.class);
        exception.expectMessage("Følgende filer er oppført i " + MD5_CHECKSUMS_FILENAME + " men finnes ikke i ");

        validator.validate();
    }


    @Test
    public void testInvalidChecksumThrowsException() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());
        File fileToCheck = grunnlagsdataBatchKatalog.toPath().resolve("test.csv").toFile();
        fileToCheck.createNewFile();
        File checksumFile = grunnlagsdataBatchKatalog.toPath().resolve(MD5_CHECKSUMS_FILENAME).toFile();
        checksumFile.createNewFile();

        write("some;checksumFile;content", fileToCheck);

        String corruptMd5Checksum = "123";
        write(corruptMd5Checksum + " *" + fileToCheck.getName(), checksumFile);

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        exception.expect(GrunnlagsdataException.class);
        exception.expectMessage("Følgende filer har en annen m5d-sjekksum enn oppgitt");

        validator.validate();
    }

    @Test
    public void testHappyday() throws Exception {
        File grunnlagsdataBatchKatalog = testFolder.newFolder(name.getMethodName());
        File fileToCheck = grunnlagsdataBatchKatalog.toPath().resolve("test.csv").toFile();
        fileToCheck.createNewFile();
        File checksumFile = grunnlagsdataBatchKatalog.toPath().resolve(MD5_CHECKSUMS_FILENAME).toFile();
        checksumFile.createNewFile();

        write("some;checksumFile;content", fileToCheck);

        write(getMd5Checksum(fileToCheck) + " *" + fileToCheck.getName(), checksumFile);

        GrunnlagsdataDirectoryValidator validator = new GrunnlagsdataDirectoryValidator(grunnlagsdataBatchKatalog.toPath());

        validator.validate();
    }

    private Path write(String content, File file) throws IOException {
        return Files.write(file.toPath(), content.getBytes(Charset.forName("cp1252")));
    }

    private String getMd5Checksum(File fileToCheck) {
        String hex;
        try (final FileInputStream input = new FileInputStream(fileToCheck)) {
            hex = DigestUtils.md5Hex(input);
        } catch (IOException e) {
            throw new GrunnlagsdataException(e);
        }
        return hex;
    }
}