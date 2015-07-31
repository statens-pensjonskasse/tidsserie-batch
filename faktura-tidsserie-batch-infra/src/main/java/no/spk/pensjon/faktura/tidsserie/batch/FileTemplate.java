package no.spk.pensjon.faktura.tidsserie.batch;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Snorre E. Brekke - Computas
 */
public class FileTemplate implements Serializable {
    private final String directory;
    private final String prefix;
    private final String suffix;

    public FileTemplate(Path directory, String prefix, String suffix) {
        this.directory = directory.toFile().getAbsolutePath();
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public File createUniqueFile() {
        return Paths.get(directory, prefix + UUID.randomUUID().toString() + suffix).toFile();
    }
}
