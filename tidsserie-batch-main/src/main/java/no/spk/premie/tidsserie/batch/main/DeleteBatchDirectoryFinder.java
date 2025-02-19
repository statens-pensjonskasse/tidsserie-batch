package no.spk.premie.tidsserie.batch.main;

import static java.nio.file.Files.walkFileTree;
import static java.util.Objects.requireNonNull;
import static no.spk.premie.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PATTERN;
import static no.spk.premie.tidsserie.batch.core.BatchIdConstants.TIDSSERIE_PREFIX;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.premie.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;

/**
 * Finner kataloger som skal slettes når batchen kjører. Dette vil være data-katalogen hvor tidsserien lagres,
 * og kataloger under log-katalogen som er eldre enn en konfigurarbart antall dager.
 *
 * @author Snorre E. Brekke - Computas
 */
public class DeleteBatchDirectoryFinder {
    private final Path dataDirectory;
    private final Path logDirectory;

    /**
     * Lager en ny {@link DeleteBatchDirectoryFinder}. {@code dataDirectory} vil alltid bli returnert fra {@link #findDeletableBatchDirectories(AldersgrenseForSlettingAvLogKatalogar)},
     *
     * @param dataDirectory katalogen hvor tidsserien blir lagret. Vil alltid bli returnert fra {@link #findDeletableBatchDirectories(AldersgrenseForSlettingAvLogKatalogar)}
     * @param logDirectory katalogen hvor det lages under-kataloger per batch-kjøring som inneholder log og metadata.  {@link DeleteBatchDirectoryFinder} vil
     * se etter underkataloger som kan slettes her.
     */
    public DeleteBatchDirectoryFinder(Path dataDirectory, Path logDirectory) {
        this.dataDirectory = requireNonNull(dataDirectory, "dataDirectory kan ikke være null.");
        this.logDirectory = requireNonNull(logDirectory, "logDirectory kan ikke være null.");
    }

    /**
     * Finner alle under-kataloger i log-katalogen som har navn på samme format som {@link BatchId} og som basert på id'en har kjørt tidligere enn
     * [{@code midnatt for &lt;i morgen&gt; - aldersgrense}]. Returnerer alltid dataDirectory.
     *
     * @param aldersgrense alle batch-kataloger som er eldre enn [{@code midnatt for &lt;i morgen&gt; - aldersgrense}] i {@code workdirectory} vil bli slettet
     * @return sti til kataloger som kan slettes
     * @throws HousekeepingException dersom filoperasjoner feiler
     */
    public Path[] findDeletableBatchDirectories(final AldersgrenseForSlettingAvLogKatalogar aldersgrense) throws HousekeepingException {
        try {
            return Stream.concat(
                    Stream.of(dataDirectory),
                    aldersgrense.finnSlettbareLogkatalogar(this::søk)
            )
                    .toArray(Path[]::new);
        } catch (final IOException e) {
            throw new HousekeepingException("walkFileTree feilet", e);
        }
    }

    private Stream<Path> søk(final LocalDateTime cutoff) throws IOException {
        final FindDeleteBatchDirectoryVisitor visitor = new FindDeleteBatchDirectoryVisitor(
                logDirectory,
                cutoff
        );
        walkFileTree(logDirectory, visitor);
        return visitor.stream();
    }

    private static class FindDeleteBatchDirectoryVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> deleteDirectories = new ArrayList<>();
        private final LocalDateTime cutoff;
        private final Path startDirectory;

        FindDeleteBatchDirectoryVisitor(final Path startDirectory, final LocalDateTime cutoff) {
            this.cutoff = requireNonNull(cutoff, "cutoff er påkrevd, men var null");
            this.startDirectory = requireNonNull(startDirectory, "startDirectory er påkrevd, men var null");
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return isDirectoryDeletable(dir, cutoff) || startDirectory.equals(dir) ?
                    FileVisitResult.CONTINUE :
                    FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            if (!startDirectory.equals(dir)) {
                deleteDirectories.add(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        Stream<Path> stream() {
            return deleteDirectories.stream();
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
