package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.batch.Validators.require;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author Snorre E. Brekke - Computas
 */
public class GrunnlagsdataDirectoryValidator {
    static final String MD5_CHECKSUMS_FILENAME = "md5-checksums.txt";

    private Path grunnlagsdataBatchKatalog;

    public GrunnlagsdataDirectoryValidator(Path grunnlagsdataBatchKatalog) {
        this.grunnlagsdataBatchKatalog = grunnlagsdataBatchKatalog;
    }

    public void validate() throws GrunnlagsdataException {
        Path checksumsFile = grunnlagsdataBatchKatalog.resolve(MD5_CHECKSUMS_FILENAME);

        assertChecksumfileExists(checksumsFile);

        Map<String, String> filenameHexMap = mapFilenamesToChecksums(checksumsFile);
        assertChecksumfileContainsValues(filenameHexMap);
        assertListedFilesExist(filenameHexMap);
        assertChecksums(filenameHexMap);
    }

    private Map<String, String> mapFilenamesToChecksums(Path checksumsFile) {
        try (final Stream<String> lines = Files.lines(checksumsFile, Charset.forName("cp1252"))) {
            return lines
                    .map(s -> s.split(" \\*"))
                    .map(s -> require(s, v -> v.length == 2, v -> new GrunnlagsdataException(MD5_CHECKSUMS_FILENAME + " er korrupt.")))
                    .collect(Collectors.toMap(s -> s[1], s -> s[0]));
        } catch (IOException e) {
            throw new GrunnlagsdataException(e);
        }
    }

    private void assertChecksumfileContainsValues(Map<String, String> filenameHexMap) {
        require(filenameHexMap,
                m -> !m.isEmpty(),
                f -> new GrunnlagsdataException(MD5_CHECKSUMS_FILENAME + " er tom."));
    }

    private void assertListedFilesExist(Map<String, String> filenameHexMap) {
        require(filenameHexMap.keySet()
                        .stream()
                        .filter(f -> !grunnlagsdataBatchKatalog.resolve(f).toFile().exists())
                        .collect(toList()),
                List::isEmpty,
                l -> new GrunnlagsdataException(
                        l.stream()
                                .collect(joining(", ", "Følgende filer er oppført i " + MD5_CHECKSUMS_FILENAME +
                                        " men finnes ikke i " + grunnlagsdataBatchKatalog.toFile().getAbsolutePath(), ""))));
    }

    private void assertChecksums(Map<String, String> filenameHexMap) {
        require(filenameHexMap.entrySet()
                        .stream()
                        .filter(e -> !e.getValue().equals(getMd5Checksum(grunnlagsdataBatchKatalog.resolve(e.getKey()).toFile())))
                        .map(Map.Entry::getKey)
                        .collect(toList()),
                List::isEmpty,
                l -> new GrunnlagsdataException(l.stream()
                        .collect(joining(", ", "Følgende filer har en annen m5d-sjekksum enn oppgitt i " + MD5_CHECKSUMS_FILENAME + ": ", ""))));
    }

    private void assertChecksumfileExists(Path checksumsFile) {
        require(checksumsFile,
                f -> f.toFile().exists(),
                f -> new GrunnlagsdataException(MD5_CHECKSUMS_FILENAME + " mangler i katalogen " + f.toFile().getAbsolutePath()));
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
