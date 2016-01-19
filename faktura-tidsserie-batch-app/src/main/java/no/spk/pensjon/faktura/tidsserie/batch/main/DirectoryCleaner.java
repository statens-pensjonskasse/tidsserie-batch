package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static no.spk.pensjon.faktura.tidsserie.core.Validators.require;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Sletter angitte kataloger og underliggende filer.
 *
 * @author Snorre E. Brekke - Computas
 * @see DeleteBatchDirectoryFinder
 */
public class DirectoryCleaner {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryCleaner.class);
    private final List<Path> baseDirectories;

    /**
     * Oppretter en DirectoryCleaner som skal slette katalog og innhold i {@code deleteDirectories }
     * @param deleteDirectories kataloger som skal slettes
     */
    public DirectoryCleaner(Path... deleteDirectories) {
        require(requireNonNull(deleteDirectories, "deleteDirectories kan ikke være null."),
                d -> d.length > 0,
                p -> new IllegalArgumentException("Må angi minst en katalog for sletting."));
        stream(deleteDirectories)
                .map(Path::toFile)
                .forEach(file -> require(
                        file,
                        f -> !f.exists() || f.isDirectory(),
                        f -> new IllegalArgumentException(f.toString() + " er ikke en katalog.")
                ));
        this.baseDirectories = Arrays.asList(deleteDirectories);
    }

    /**
     * Sletter alle batch-kataloger generert av tidligere kjøringer av faktura-tidsserie-batch.
     * @throws HousekeepingException dersom filoperasjoner feiler
     */
    public void deleteDirectories() throws HousekeepingException {
        logger.info("Sletter alle angitte kataloger.");
        for (Path directory : baseDirectories) {
            if (directory.toFile().exists()) {
                try {
                    Files.walkFileTree(directory, new BatchDirectoryDeleteVisitor());
                } catch (IOException e) {
                    logger.error("walkFileTree feilet", e);
                    throw new HousekeepingException("walkFileTree feilet", e);
                }
            }
        }
    }

    private static class BatchDirectoryDeleteVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            tryDelete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            logger.info("Sletter " + dir);
            tryDelete(dir);
            return FileVisitResult.CONTINUE;
        }

        private void tryDelete(Path path) throws IOException {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.warn("Sletting feilet", e);
                throw e;
            }
        }


    }
}
