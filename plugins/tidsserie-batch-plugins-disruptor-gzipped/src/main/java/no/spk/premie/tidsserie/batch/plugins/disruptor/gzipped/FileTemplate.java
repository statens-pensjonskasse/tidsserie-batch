package no.spk.premie.tidsserie.batch.plugins.disruptor.gzipped;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileTemplate implements Serializable {
    @Serial
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
