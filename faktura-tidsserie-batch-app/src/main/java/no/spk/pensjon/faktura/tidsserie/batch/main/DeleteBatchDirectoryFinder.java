package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Objects.requireNonNull;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.TIDSSERIE_PATTERN;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.TIDSSERIE_PREFIX;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import no.spk.faktura.input.BatchId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finner kataloger som skal slettes når batchen kjører. Dette vil være data-katalogen hvor tidsserien lagres,
 * og kataloger under log-katalogen som er eldre enn en konfigurarbart antall dager.
 * @author Snorre E. Brekke - Computas
 */
public class DeleteBatchDirectoryFinder {

    private static final Logger logger = LoggerFactory.getLogger(DeleteBatchDirectoryFinder.class);

    private final Path dataDirectory;
    private final Path logDirectory;

    /**
     * Lager en ny {@link DeleteBatchDirectoryFinder}. {@code dataDirectory} vil alltid bli returnert fra {@link #findDeletableBatchDirectories(int)},
     * @param dataDirectory katalogen hvor tidsserien blir lagret. Vil alltid bli returnert fra {@link #findDeletableBatchDirectories(int)}
     * @param logDirectory katalogen hvor det lages under-kataloger per batch-kjøring som inneholder log og metadata.  {@link DeleteBatchDirectoryFinder} vil
     * se etter underkataloger som kan slettes her.
     */
    public DeleteBatchDirectoryFinder(Path dataDirectory, Path logDirectory) {
        this.dataDirectory = requireNonNull(dataDirectory, "dataDirectory kan ikke være null.");
        this.logDirectory = requireNonNull(logDirectory, "logDirectory kan ikke være null.");
    }

    /**
     * Finner alle under-kataloger i log-katalogen som har navn på samme format som {@link BatchId} og som basert på id'en har kjørt tidligere enn
     * [{@code midnatt for &lt;i morgen&gt; - olderThanDays}]. Returnerer alltid dataDirectory.
     * @param olderThanDays alle batch-kataloger som er eldre enn [{@code midnatt for &lt;i morgen&gt; - olderThanDays}] i {@code workdirectory} vil bli slettet
     * @return sti til kataloger som kan slettes
     * @throws HousekeepingException dersom filoperasjoner feiler
     */
    public Path[] findDeletableBatchDirectories(int olderThanDays) throws HousekeepingException{
        List<Path> deleteDirectories = new ArrayList<>();
        deleteDirectories.add(dataDirectory);

        if (olderThanDays > 0) {
            Path directory =  logDirectory;
            final LocalDateTime cutoff = getMidnightCutoff(olderThanDays);
            try {
                Files.walkFileTree(directory, new FindDeleteBatchDirectoryVisitor(directory, deleteDirectories, cutoff));
            } catch (IOException e) {
                logger.error("walkFileTree feilet", e);
                throw new HousekeepingException("walkFileTree feilet", e);
            }
        }

        return deleteDirectories.toArray(new Path[deleteDirectories.size()]);
    }

    private LocalDateTime getMidnightCutoff(int olderThanDays) {
        return LocalDate.now().atStartOfDay().plusDays(1).minusDays(olderThanDays);
    }

    private static class FindDeleteBatchDirectoryVisitor extends SimpleFileVisitor<Path> {
        private final LocalDateTime cutoff;
        private final Path startDirectory;
        private final List<Path> deleteDirectories;

        public FindDeleteBatchDirectoryVisitor(Path startDirectory, List<Path> deleteDirectories, LocalDateTime cutoff) {
            this.cutoff = cutoff;
            this.startDirectory = startDirectory;
            this.deleteDirectories = deleteDirectories;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return isDirectoryDeletable(dir, cutoff) || startDirectory.equals(dir)?
                    FileVisitResult.CONTINUE :
                    FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (!startDirectory.equals(dir)) {
                deleteDirectories.add(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        private boolean isDirectoryDeletable(Path dir, LocalDateTime cutoff) {
            Matcher matcher = TIDSSERIE_PATTERN.matcher(dir.toFile().getName());
            if (matcher.matches()) {
                String batchIdString = matcher.group(0);
                BatchId batchId = BatchId.fromString(TIDSSERIE_PREFIX, batchIdString);
                LocalDateTime batchTimestamp = batchId.asLocalDateTime();
                return batchTimestamp.isBefore(cutoff);

            }
            return false;
        }
    }
}
