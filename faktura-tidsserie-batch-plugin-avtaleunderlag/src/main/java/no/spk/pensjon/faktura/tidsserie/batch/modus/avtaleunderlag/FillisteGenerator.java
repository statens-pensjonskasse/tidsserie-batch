package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.lang.Math.max;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import no.spk.pensjon.faktura.tidsserie.batch.core.StorageBackend;

/**
 * Finner alle tidsserie*.csv filer i utkatalog, og lagrer filnavnene i 1 filliste-fil.
 * <br>
 * Fillisten navngis på formen {@code FFF_FILLISTE_1.txt}.
 * <br>
 * Ettersom samme type lagringsbackend benyttes både ved distribuert generering av tidsserie (stillingsforholdobservasjonar, live_tidsserie og avregning)
 * og ved lokal generering av tidsserie (avtaleunderlag), blir tidsseriefiler dynamisk navngitt for å unngå navnekonflikter ved distribuert generering.
 * <br>
 * Formålet med fillisten er å la Datavarehus angi et fast filnavn i workflowene som skal lese inn avtaleunderlaget.
 * <br>
 * I motsetning til for live_tidsserie-modusen, vil det ikke bli opprettet noen dummy-fillister for avtaleunderlaget dersom antall tidsseriefiler
 * er mindre enn minste mulige antall tidsseriefiler (1).
 *
 * @author Snorre E. Brekke - Computas
 * @author Tarjei Skorgenes
 */
class FillisteGenerator {
    final static Pattern CSV_PATTERN = Pattern.compile("^tidsserie.+\\.csv$");

    private static final int GROUP_FILE_COUNT = 1;

    /**
     * Genererer filliste for avtaleunderlaget og lagrer fillisten i {@code utKatalog} under navnet {@code FFF_FILLISTE_1.txt}.
     *
     * @param utKatalog ut-katalogen batchen skriver den dynamisk navngitte CSV-filen som inneholder avtaleunderlaget, til.
     * Fillisten blir også lagret til denne katalogen
     */
    void genererFilliste(final Path utKatalog) {
        File[] csvFiles = utKatalog.toFile()
                .listFiles(f -> CSV_PATTERN.matcher(f.getName()).matches());

        IntSupplier fileNumberSupplier = createGroupFiles(utKatalog, GROUP_FILE_COUNT);

        for (File csvFile : csvFiles) {
            appendCsvFilenameToGroup(utKatalog, csvFile, fileNumberSupplier);
        }

        createDummyCsvFiles(dataKatalog, getDummyFileCount(csvFiles), fileNumberSupplier);

    }

    private int getDummyFileCount(File[] csvFiles) {
        return max(0, GROUP_FILE_COUNT - csvFiles.length);
    }

    private void appendCsvFilenameToGroup(Path dataKatalog, File csvFile, IntSupplier fileNumberSupplier) {
        Path groupFile = dataKatalog.resolve(getGroupFileName(fileNumberSupplier.getAsInt()));
        appendCsvFilename(csvFile, groupFile);
    }

    private void createDummyCsvFiles(Path dataKatalog, int dummyCsvFileCount, IntSupplier fileNumberSupplier) {
        if (dummyCsvFileCount > 0) {
            for (int i = 0; i < dummyCsvFileCount; i++) {
                Path dummyCsv = createDummyCsvFile(dataKatalog, i);
                appendCsvFilenameToGroup(dataKatalog, dummyCsv.toFile(), fileNumberSupplier);
            }
        }
    }

    private Path createDummyCsvFile(Path dataKatalog, int index) {
        Path dummyCsv = dataKatalog.resolve("tidsserie_dummy_" + index + ".csv");
        try {
            Files.createFile(dummyCsv);
        } catch (IOException e) {
            throw new UncheckedIOException("Kunne ikke opprette " + dummyCsv.toString(), e);
        }
        return dummyCsv;
    }

    private IntSupplier createGroupFiles(Path dataKatalog, int fileCount) {
        IntSupplier fileNumberSupplier = new IntSupplier() {
            int current = 0;

            public int getAsInt() {
                return (current++ % fileCount) + 1;
            }
        };

        IntStream.generate(fileNumberSupplier)
                .limit(fileCount)
                .forEach(i -> createGroupFile(dataKatalog, i));

        return fileNumberSupplier;
    }

    private void createGroupFile(Path dataKatalog, int fileNumber) {
        Path groupFile = dataKatalog.resolve(getGroupFileName(fileNumber));
        try {
            Files.createFile(groupFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Skriving til " + groupFile.toString() + " feilet.", e);
        }
    }

    private String getGroupFileName(int fileNumber) {
        return "FFF_FILLISTE_" + fileNumber + ".txt";
    }

    private void appendCsvFilename(File csvFile, Path groupFile) {
        try (FileWriter fileWriter = new FileWriter(groupFile.toFile(), true)) {
            fileWriter.append(csvFile.getName()).append("\n");
        } catch (IOException e) {
            throw new UncheckedIOException("Skriving til " + groupFile.toString() + " feilet.", e);
        }
    }
}