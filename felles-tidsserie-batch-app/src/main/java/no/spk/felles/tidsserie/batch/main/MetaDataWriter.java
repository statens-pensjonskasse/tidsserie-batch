package no.spk.felles.tidsserie.batch.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import no.spk.faktura.input.BatchId;
import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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
            Template template = config.getTemplate("metadata.ftl", Locale.forLanguageTag("no"), "utf-8");
            template.process(dataModel, writer);
        } catch (IOException | TemplateException e) {
            logger.error("Klarte ikke å opprette metadata-fil", e);
            return Optional.empty();
        }
        return Optional.of(fileToWrite);
    }

    private String getDurationString(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
