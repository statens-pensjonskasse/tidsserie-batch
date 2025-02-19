package no.spk.premie.tidsserie.batch.plugins.disruptor;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Snorre E. Brekke - Computas
 */
public class FileTemplate implements Serializable {
    private final static long serialVersionUID = 1;

    private final String directory;
    private final String suffix;

    public FileTemplate(Path directory, String suffix) {
        this.directory = directory.toFile().getAbsolutePath();
        this.suffix = suffix;
    }

    public File createUniqueFile(final long serienummer, final String filprefix) {
        return Paths.get(
                directory,
                filprefix
                        + serienummer
                        + "-"
                        + UUID.randomUUID()
                        + suffix
        ).toFile();
    }
}
