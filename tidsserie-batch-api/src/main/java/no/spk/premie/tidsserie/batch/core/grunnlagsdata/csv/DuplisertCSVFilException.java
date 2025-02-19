package no.spk.premie.tidsserie.batch.core.grunnlagsdata.csv;

import java.io.File;
import java.nio.file.Path;

class DuplisertCSVFilException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final File file;

    @Override
    public String getMessage() {
        return String.format(
                "Det kan ikkje eksistere både en komprimert og ukomprimert versjon av filer i uttrekket, men %s har to slike.\nKomprimert fil: %s\nUkomprimert fil: %s",
                file.getName(),
                file(file, ".csv.gz"),
                file(file, ".csv")
        );
    }

    static Path sjekkForDuplikat(final Path path) {
        final File file = path.toFile();
        if (exists(file, ".csv") && exists(file, ".csv.gz")) {
            throw new DuplisertCSVFilException(file);
        }
        return file.toPath();
    }

    private DuplisertCSVFilException(final File file) {
        this.file = file;
    }

    private static boolean exists(final File file, final String extension) {
        return file(file, extension).exists();
    }

    private static File file(final File file, final String extension) {
        return new File(
                file.getParentFile(),
                prefix(file.getName()) + extension
        );
    }

    private static String prefix(final String filename) {
        return filename
                .replaceAll(".gz", "")
                .replaceAll(".csv", "");
    }
}
