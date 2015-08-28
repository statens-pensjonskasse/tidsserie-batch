package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Har i oppgrave å generere metadata.txt fil for batchen som gir en oppsummering av parametre og status på kjøringen.
 * I tilegg kan klassen produserere checksums.txt som inneholder checksummer for alle .csv filer som blir lagret
 * i arbeidskatalogen for batchen.
 *
 * @author Snorre E. Brekke - Computas
 */
public class MetaDataWriter {
    private static final String MD5_CHECKSUMS_FILENAME = "md5-checksums.txt";
    private static final String LOG_FILENAME = "batch.log";

    private Logger logger = LoggerFactory.getLogger(MetaDataWriter.class);
    private final Configuration config;

    private final Path batchKatalog;

    public MetaDataWriter(Configuration config, Path batchKatalog) {
        this.config = config;
        this.batchKatalog = batchKatalog;
    }

    public Optional<File> createMetadataFile(ProgramArguments programArguments, BatchId batchId, Duration duration) {
        File fileToWrite = batchKatalog.resolve("metadata.txt").toFile();
        try (Writer writer = new FileWriter(fileToWrite)) {
            HashMap<String, Object> dataModel = new HashMap<>();
            dataModel.put("params", programArguments);
            dataModel.put("batchId", batchId);
            dataModel.put("jobDuration", getDurationString(duration));
            dataModel.put("outputDirectory", batchKatalog.toAbsolutePath().normalize().toString());
            Template template = config.getTemplate("metadata.ftl");
            template.process(dataModel, writer);
        } catch (IOException | TemplateException e) {
            logger.error("Klarte ikke å opprette metadata-fil", e);
            return Optional.empty();
        }
        return Optional.of(fileToWrite);
    }

    public Optional<File> createChecksumFile(Path... forFilesInDirectories) {
        File fileToWrite = batchKatalog.resolve(MD5_CHECKSUMS_FILENAME).toFile();
        try (Writer writer = new FileWriter(fileToWrite)) {
            Stream<File> files = getFilesToChecksum(concat(stream(forFilesInDirectories), of(batchKatalog)));
            files.forEach(file -> {
                try {
                    try (final FileInputStream input = new FileInputStream(file)) {
                        String hex = DigestUtils.md5Hex(input);
                        writer.append(hex).append(" *").append(file.getName()).append("\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            logger.error("Klarte ikke å opprette checksum-fil", e);
            return Optional.empty();
        }

        return Optional.of(fileToWrite);
    }

    /**
     * Oppretter ok.trg i batchkatalogen.
     */
    public void createTriggerFile(Path utKatalog) {
        Path resolve = utKatalog.resolve("ok.trg");
        try {
            Files.createFile(resolve);
        } catch (IOException e) {
            throw new UncheckedIOException("Klarte ikke å opprette triggerfil.", e);
        }
    }

    /**
     * Finner alle tidsserie*.csv filer i utkatalog, og fordeler filnavmeme i ti filer: FFF_FILLISTE_[1-10].txt.
     * Filliste-filene brukes slik at Datavarehus kan bruke faste filnavn for å paralellisere innlesingen av csv-filene.
     * @param dataKatalog katalog med tidsserie*.csv filer. Katalogen vil inneholder 10 filer FFF_FILLISTE_[1-10].txt etter kjøring.
     */
    public void createCsvGroupFiles(Path dataKatalog) {
        new CsvFileGroupWriter().createCsvGroupFiles(dataKatalog);
    }

    private Stream<File> getFilesToChecksum(Stream<Path> directoryNames) {
        return directoryNames
                .map(Path::toFile)
                .map(f -> f.listFiles(((file) -> !Arrays.asList(LOG_FILENAME, MD5_CHECKSUMS_FILENAME).contains(file.getName()) && file.isFile())))
                .map(Arrays::stream)
                .flatMap(s -> s);
    }

    private String getDurationString(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
