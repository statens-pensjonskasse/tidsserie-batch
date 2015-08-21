package no.spk.pensjon.faktura.tidsserie.batch.main;

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

    public CsvFileGroupWriter() {
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     * @param dataKatalog katalog med tidsserie*.csv filer. Katalogen vil inneholder 10 filer FFF_FILLISTE_[1-10].txt etter kjøring.
     */
    public void createCsvGroupFiles(Path dataKatalog) {
        File[] csvFiles = dataKatalog.toFile()
                .listFiles(f -> CSV_PATTERN.matcher(f.getName()).matches());

        final int groupFileCount = 10;
        createGroupFiles(dataKatalog, groupFileCount);
        int currentFilenumber = 1;
        for (File csvFile : csvFiles) {
            Path groupFile = dataKatalog.resolve(getGroupFileName(currentFilenumber));
            appendCsvFilename(csvFile, groupFile);
            currentFilenumber = nextFileNumber(currentFilenumber, groupFileCount);
        }

    }

    void createGroupFiles(Path dataKatalog, int groupFileCount) {
        for (int i = 1; i <= groupFileCount; i++) {
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

    int nextFileNumber(int currentFile, int groupFileCount) {
        currentFile++;
        if (currentFile > groupFileCount) {
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