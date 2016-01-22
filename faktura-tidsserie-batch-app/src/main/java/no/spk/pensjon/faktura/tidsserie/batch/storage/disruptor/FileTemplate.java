package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

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
    private final String prefix;
    private final String suffix;

    public FileTemplate(Path directory, String prefix, String suffix) {
        this.directory = directory.toFile().getAbsolutePath();
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public File createUniqueFile(final long serienummer) {
        return Paths.get(
                directory,
                prefix
                        + serienummer
                        + "-"
                        + UUID.randomUUID()
                        + suffix
        ).toFile();
    }
}
