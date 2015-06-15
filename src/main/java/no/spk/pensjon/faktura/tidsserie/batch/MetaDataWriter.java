package no.spk.pensjon.faktura.tidsserie.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.storage.main.BatchId;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Har i oppgrave � generere metadata.txt fil for batchen som gir en oppsummering av parametre og status p� kj�ringen.
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

    public Optional<File> createMetadataFile(ProgramArguments programArguments, BatchId batchId) {
        File fileToWrite = batchKatalog.resolve("metadata.txt").toFile();
        try (Writer writer = new FileWriter(fileToWrite)) {
            HashMap<String, Object> dataModel = new HashMap<>();
            dataModel.put("params", programArguments);
            dataModel.put("batchId", batchId);
            dataModel.put("outputDirectory", batchKatalog.toString());
            Template template = config.getTemplate("metadata.ftl");
            template.process(dataModel, writer);
        } catch (IOException | TemplateException e) {
            logger.error("Klarte ikke � opprette metadata-fil", e);
            return Optional.empty();
        }
        return Optional.of(fileToWrite);
    }

    public Optional<File> createChecksumFile() {
        File fileToWrite = batchKatalog.resolve(MD5_CHECKSUMS_FILENAME).toFile();
        try (Writer writer = new FileWriter(fileToWrite)) {
            File[] files = getFiles(batchKatalog);
            if (files != null) {
                for (File file : files) {
                    try {
                        try (final FileInputStream input = new FileInputStream(file)) {
                            String hex = DigestUtils.md5Hex(input);
                            writer.append(hex).append(" *").append(file.getName()).append("\n");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Klarte ikke � opprette checksum-fil", e);
            return Optional.empty();
        }

        return Optional.of(fileToWrite);
    }

    private File[] getFiles(Path directoryName) {
        File directory = directoryName.toFile();
        return directory.listFiles((file) -> !Arrays.asList(LOG_FILENAME, MD5_CHECKSUMS_FILENAME).contains(file.getName()) && file.isFile());
    }
}
