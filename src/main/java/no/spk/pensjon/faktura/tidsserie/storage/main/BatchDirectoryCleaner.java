package no.spk.pensjon.faktura.tidsserie.storage.main;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Klassen er ansvarlig for � slette batch-kataloger i en arbeidskatalog som er eldre enn n-antall dager.
 * Kataloger som har navn link formatet til {@link BatchId} vil bli parset til en dato, og sammenlignet med �nsket slette-tidspunkt.
 * <p>Dersom katalogen har et navn som tilsier at batchen kj�rte tidligere enn [{@code midnatt for &lt;i morgen&gt; - eldreEnnDager}]
 * blir katalogen og innholdet slettet.</p>
 *
 * @author Snorre E. Brekke - Computas
 * @see BatchId
 */
public class BatchDirectoryCleaner {

    private static final Logger logger = LoggerFactory.getLogger(BatchDirectoryCleaner.class);
    private static final Pattern FILENAME_PATTERN = BatchIdMatcher.createBatchIdPattern(BatchId.ID_PREFIX);

    private final Path workDirectory;

    /**
     * Oppretter en BatchDirectoryCleaner som skal slette innhold i {@code utKatalog }
     *
     * @param utKatalog rot-katalogen som innholder batchkataloger som skal slettes
     */
    public BatchDirectoryCleaner(Path utKatalog) {
        this.workDirectory = requireNonNull(utKatalog);
    }

    /**
     * Sletter alle batch-kataloger generert av tidligere kj�ringer av faktura-tidsserie-batch.
     *
     * @return {@link Oppryddingsstatus} inneholder eventuelle feilmeldinger som skjedde under slettingen
     */
    public Oppryddingsstatus deleteAllPreviousBatches() {
        Oppryddingsstatus oppryddingsstatus = new Oppryddingsstatus();

        Path directory = workDirectory;
        logger.info("Sletter alle tidligere batch-kataloger.");
        try {
            Files.walkFileTree(directory, new BatchDirectoryDeleteVisitor(directory, oppryddingsstatus));
        } catch (IOException e) {
            oppryddingsstatus.addError("walkFileTree feilet", e);
        }

        return oppryddingsstatus;
    }

    private static class BatchDirectoryDeleteVisitor extends SimpleFileVisitor<Path> {
        private final Path startDirectory;
        private final Oppryddingsstatus oppryddingsstatus;

        public BatchDirectoryDeleteVisitor(Path startDirectory, Oppryddingsstatus oppryddingsstatus) {
            this.startDirectory = startDirectory;
            this.oppryddingsstatus = oppryddingsstatus;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return isDirectoryDeletable(dir) || startDirectory.equals(dir) ?
                    FileVisitResult.CONTINUE :
                    FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!file.getParent().equals(startDirectory)) {
                tryDelete(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (!dir.equals(startDirectory)) {
                logger.info("Sletter " + dir);
                tryDelete(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        private void tryDelete(Path path) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.warn("Sletting feilet", e);
                if (path.toFile().isDirectory()) {
                    oppryddingsstatus.addError(path.toString(), e);
                }
            }
        }

        private boolean isDirectoryDeletable(Path dir) {
            return FILENAME_PATTERN.matcher(dir.toFile().getName()).matches();
        }
    }
}
