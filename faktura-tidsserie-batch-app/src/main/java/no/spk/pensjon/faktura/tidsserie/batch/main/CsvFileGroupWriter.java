package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.lang.Math.max;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

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

        createGroupFiles(dataKatalog);

        int currentFilenumber = 1;
        for (File csvFile : csvFiles) {
            currentFilenumber = appendCsvFilenameToGroup(currentFilenumber, dataKatalog, csvFile);
        }

        int dummyCsvFileCount = max(0, GROUP_FILE_COUNT - csvFiles.length);
        createDummyCsvFiles(dataKatalog, dummyCsvFileCount, currentFilenumber);

    }

    private int appendCsvFilenameToGroup(int currentFilenumber, Path dataKatalog, File csvFile) {
        Path groupFile = dataKatalog.resolve(getGroupFileName(currentFilenumber));
        appendCsvFilename(csvFile, groupFile);
        currentFilenumber = nextFileNumber(currentFilenumber);
        return currentFilenumber;
    }

    private void createDummyCsvFiles(Path dataKatalog, int dummyCsvFileCount, int currentFilenumber) {
        if (dummyCsvFileCount > 0) {

            for (int i = 0; i < dummyCsvFileCount; i++) {
                Path dummyCsv = createDummyCsvFile(dataKatalog, i);
                currentFilenumber = appendCsvFilenameToGroup(currentFilenumber, dataKatalog, dummyCsv.toFile());
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

    void createGroupFiles(Path dataKatalog) {
        for (int i = 1; i <= GROUP_FILE_COUNT; i++) {
            Path groupFile = dataKatalog.resolve(getGroupFileName(i));
            try {
                Files.createFile(groupFile);
            } catch (IOException e) {
                throw new TidsserieException("Skriving til " + groupFile.toString() + " feilet.");
            }
        }
    }

    String getGroupFileName(int fileNumber) {
        return "FFF_FILLISTE_" + fileNumber + ".txt";
    }

    int nextFileNumber(int currentFile) {
        currentFile++;
        if (currentFile > GROUP_FILE_COUNT) {
            currentFile = 1;
        }
        return currentFile;
    }

    void appendCsvFilename(File csvFile, Path groupFile) {
        try (FileWriter fileWriter = new FileWriter(groupFile.toFile(), true)) {
            fileWriter.append(csvFile.getName()).append("\n");
        } catch (IOException e) {
            throw new TidsserieException("Skriving til " + groupFile.toString() + " feilet.");
        }
    }
}