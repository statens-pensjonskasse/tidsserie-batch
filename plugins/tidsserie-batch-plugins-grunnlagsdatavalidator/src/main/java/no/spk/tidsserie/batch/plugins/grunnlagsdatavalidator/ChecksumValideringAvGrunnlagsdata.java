package no.spk.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static no.spk.tidsserie.batch.core.Validators.require;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import no.spk.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;

/**
 * {@link ChecksumValideringAvGrunnlagsdata} verifiserer at filene i grunnlagsdatakatalogen ikkje har blitt endra
 * sidan dei vart generert.
 * <br>
 * Dette blir verifisert ved å sjekke om MD5-sjekksummane for alle filene framleis matchar det dei var når PU_FAK_BA_09
 * genererte filene.
 */
public class ChecksumValideringAvGrunnlagsdata implements UttrekksValidator {
    static final String MD5_CHECKSUMS_FILENAME = "md5-checksums.txt";
    private static final int INDEX_SJEKKSUM = 0;
    private static final int INDEX_FILNAVN = 1;

    private final Md5sum md5sum = new Md5sum();

    private final Path uttrekk;

    ChecksumValideringAvGrunnlagsdata(final Path uttrekk) {
        this.uttrekk = requireNonNull(uttrekk, "uttrekk er påkrevd, men var null");
    }

    @Override
    public void validate() throws UgyldigUttrekkException {
        final Path sjekksummar = uttrekk.resolve(MD5_CHECKSUMS_FILENAME);
        assertChecksumfileExists(sjekksummar);

        final Map<String, String> forventaSjekksumPrFil = lesForventningar(sjekksummar);
        assertChecksumfileContainsValues(forventaSjekksumPrFil);
        assertListedFilesExist(forventaSjekksumPrFil);
        assertChecksums(forventaSjekksumPrFil);
    }

    private Map<String, String> lesForventningar(final Path checksumsFile) {
        try (final Stream<String> lines = Files.lines(checksumsFile, StandardCharsets.UTF_8)) {
            return lines
                    .map(linje -> linje.split(" \\*"))
                    .map(
                            rad -> require(
                                    rad,
                                    kolonner -> kolonner.length == 2,
                                    kolonner -> new UgyldigUttrekkException(MD5_CHECKSUMS_FILENAME + " er korrupt.")
                            )
                    )
                    .collect(
                            toMap(
                                    rad -> rad[INDEX_FILNAVN],
                                    rad -> rad[INDEX_SJEKKSUM]
                            )
                    );
        } catch (final IOException e) {
            throw new UgyldigUttrekkException(e);
        }
    }

    private void assertChecksumfileContainsValues(final Map<String, String> forventaSjekksumPrFil) {
        require(
                forventaSjekksumPrFil,
                m -> !m.isEmpty(),
                f -> new UgyldigUttrekkException(MD5_CHECKSUMS_FILENAME + " er tom.")
        );
    }

    private void assertListedFilesExist(final Map<String, String> forventaSjekksumPrFil) {
        require(
                forventaSjekksumPrFil
                        .keySet()
                        .stream()
                        .filter(f -> !uttrekk.resolve(f).toFile().exists())
                        .toList(),
                List::isEmpty,
                manglendeFiler -> new UgyldigUttrekkException(
                        manglendeFiler
                                .stream()
                                .map(navn -> "  - " + navn)
                                .sorted()
                                .collect(
                                        joining(
                                                "\n",
                                                format(
                                                        "Følgende filer er oppført i %s men finnes ikke i %s:\n",
                                                        MD5_CHECKSUMS_FILENAME,
                                                        uttrekk.toFile().getAbsolutePath()
                                                ),
                                                ""
                                        )
                                )
                )
        );
    }

    private void assertChecksums(final Map<String, String> forventaSjekksumPrFil) {
        require(
                forventaSjekksumPrFil
                        .entrySet()
                        .stream()
                        .filter(e -> !stemmerSjekksummen(e.getKey(), e.getValue()))
                        .map(Map.Entry::getKey)
                        .toList(),
                List::isEmpty,
                filerMedFeilSjekksum -> new UgyldigUttrekkException(
                        filerMedFeilSjekksum
                                .stream()
                                .map(navn -> "  - " + navn)
                                .sorted()
                                .collect(
                                        joining(
                                                "\n",
                                                format(
                                                        "Følgende filer har en annen m5d-sjekksum enn oppgitt i %s:\n",
                                                        MD5_CHECKSUMS_FILENAME
                                                ),
                                                ""
                                        )
                                )
                )
        );
    }

    private void assertChecksumfileExists(Path checksumsFile) {
        require(
                checksumsFile,
                f -> f.toFile().exists(),
                f -> new UgyldigUttrekkException(MD5_CHECKSUMS_FILENAME + " mangler i katalogen " + f.toFile().getAbsolutePath())
        );
    }

    private boolean stemmerSjekksummen(final String filnavn, final String forventaSjekksum) {
        final String faktiskSjekksum = md5sum.produser(uttrekk.resolve(filnavn).toFile());
        return forventaSjekksum.equals(faktiskSjekksum);
    }
}
