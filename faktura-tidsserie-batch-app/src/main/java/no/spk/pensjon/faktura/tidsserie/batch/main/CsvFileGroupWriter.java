package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.lang.Math.max;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
 * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
 *
 * @author Snorre E. Brekke - Computas
 */
public class CsvFileGroupWriter {
    final static Pattern CSV_PATTERN = Pattern.compile("^tidsserie.+\\.csv$");
    private static final int GROUP_FILE_COUNT = 10;

    public CsvFileGroupWriter() {
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     * <p>
     * Dersom det er færre enn 10 tidsserie.*csv filer i dataKatalogen, vil det bli opprettet en {@code tidsserie_dummy_*.csv}-filer,
     * som vil bli referert i resternede FFF_FILLISTE_ filer.
     * Dette gjøres for å forenkle innlesingen for datavarehus.
     * </p>
     *
     * @param dataKatalog katalog med tidsserie*.csv filer. Katalogen vil inneholder 10 filer FFF_FILLISTE_[1-10].txt etter kjøring.
     */
    public void createCsvGroupFiles(Path dataKatalog) {
        File[] csvFiles = dataKatalog.toFile()
                .listFiles(f -> CSV_PATTERN.matcher(f.getName()).matches());

        IntSupplier fileNumberSupplier = createGroupFiles(dataKatalog, GROUP_FILE_COUNT);

        for (File csvFile : csvFiles) {
            appendCsvFilenameToGroup(dataKatalog, csvFile, fileNumberSupplier);
        }

        createDummyCsvFiles(dataKatalog, getDummyFileCount(csvFiles), fileNumberSupplier);

    }

    private int getDummyFileCount(File[] csvFiles) {
        return max(0, GROUP_FILE_COUNT - csvFiles.length);
    }

    private void appendCsvFilenameToGroup(Path dataKatalog , File csvFile, IntSupplier fileNumberSupplier) {
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
            throw new TidsserieException("Kunne ikke opprette " + dummyCsv.toString());
        }
        return dummyCsv;
    }

    IntSupplier createGroupFiles(Path dataKatalog, int fileCount) {
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
            throw new TidsserieException("Skriving til " + groupFile.toString() + " feilet.");
        }
    }

    String getGroupFileName(int fileNumber) {
        return "FFF_FILLISTE_" + fileNumber + ".txt";
    }

    void appendCsvFilename(File csvFile, Path groupFile) {
        try (FileWriter fileWriter = new FileWriter(groupFile.toFile(), true)) {
            fileWriter.append(csvFile.getName()).append("\n");
        } catch (IOException e) {
            throw new TidsserieException("Skriving til " + groupFile.toString() + " feilet.");
        }
    }
}