package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Klassen er ansvarlig for å slette batch-kataloger i en arbeidskatalog som er eldre enn n-antall dager.
 * Kataloger som har navn link formatet til {@link BatchId} vil bli parset til en dato, og sammenlignet med ønsket slette-tidspunkt.
 * <p>Dersom katalogen har et navn som tilsier at batchen kjørte tidligere enn [{@code midnatt for &lt;i morgen&gt; - eldreEnnDager}]
 * blir katalogen og innholdet slettet.</p>
 *
 * @author Snorre E. Brekke - Computas
 * @see BatchId
 */
public class BatchDirectoryCleaner {

    private static final Logger logger = LoggerFactory.getLogger(BatchDirectoryCleaner.class);
    private static final Pattern FILENAME_PATTERN = BatchIdMatcher.createBatchIdPattern(BatchId.ID_PREFIX);

    private final Path workDirectory;
    private final BatchId batchId;

    /**
     * Oppretter en BatchDirectoryCleaner som skal slette innhold i {@code utKatalog }
     *
     * @param utKatalog rot-katalogen som innholder batchkataloger som skal slettes
     * @param batchId id for batchen - skal ikke slette arbeidskatalogen for gjeldende kjøring
     */
    public BatchDirectoryCleaner(Path utKatalog, BatchId batchId) {
        this.workDirectory = requireNonNull(utKatalog);
        this.batchId = batchId;
    }

    /**
     * Sletter alle batch-kataloger generert av tidligere kjøringer av faktura-tidsserie-batch.
     *
     * @return {@link Oppryddingsstatus} inneholder eventuelle feilmeldinger som skjedde under slettingen
     */
    public Oppryddingsstatus deleteAllPreviousBatches() {
        Oppryddingsstatus oppryddingsstatus = new Oppryddingsstatus();

        Path directory = workDirectory;
        logger.info("Sletter alle tidligere batch-kataloger.");
        try {
            Files.walkFileTree(directory, new BatchDirectoryDeleteVisitor(directory, batchId, oppryddingsstatus));
        } catch (IOException e) {
            oppryddingsstatus.addError("walkFileTree feilet", e);
        }

        return oppryddingsstatus;
    }

    private static class BatchDirectoryDeleteVisitor extends SimpleFileVisitor<Path> {
        private final Path startDirectory;
        private final Oppryddingsstatus oppryddingsstatus;
        private final List<Path> doNotdelete;

        public BatchDirectoryDeleteVisitor(Path startDirectory, BatchId batchId, Oppryddingsstatus oppryddingsstatus) {
            this.startDirectory = startDirectory;
            this.oppryddingsstatus = oppryddingsstatus;
            doNotdelete = Arrays.asList(startDirectory, batchId.tilArbeidskatalog(startDirectory));
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
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            logger.warn("Visit file failed for " + file, exc);
            oppryddingsstatus.addError(file.toString(), exc);
            return FileVisitResult.SKIP_SUBTREE;
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
            return !doNotdelete.contains(dir) && FILENAME_PATTERN.matcher(dir.toFile().getName()).matches();
        }
    }
}
