package no.spk.pensjon.faktura.tidsserie.batch.it;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;

import org.assertj.core.api.AbstractCharSequenceAssert;

class OutOfProcessBatchRunner implements PU_FAK_BA_10 {
    @Override
    public void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus) {
        try {
            final File stderr = new File(utKatalog, "stderr");
            final File stdout = new File(utKatalog, "stdout");
            final Process process = new ProcessBuilder(
                    "java",
                    "-Dfile.encoding=" + Charset.defaultCharset().name(),
                    "-jar",
                    finnBatchensJarfil(),
                    "-i", innKatalog.toString(),
                    "-o", utKatalog.getPath(),
                    "-log", utKatalog.getPath(),
                    "-b", modus.toString(),
                    "-m", modus.kode(),
                    "-fraAar", "" + periode.fraOgMed().getYear(),
                    "-tilAar", "" + periode.tilOgMed().get().getYear(),
                    "-n", "1" // Speedar opp køyringa astronomisk mykje sidan vi ikkje ønskjer å vente på slave-shutdown med failover og partisjons-rebalansering
            )
                    .redirectError(stderr)
                    .redirectOutput(stdout)
                    .start();
            assertThat(process.waitFor(5000, TimeUnit.SECONDS))
                    .as("vart batchen ferdig i løpet av 5 sekund?")
                    .isTrue();

            assertContentOf(stderr).isEmpty();
            assertContentOf(stdout).contains("Tidsserie-batch avsluttet OK");

            assertThat(process.exitValue())
                    .as("exitCode frå batchkøyringa")
                    .isEqualTo(0);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final InterruptedException e) {
            // Be a good JVM citizen
            Thread.currentThread().interrupt();
        }
    }

    private String finnBatchensJarfil() {
        try {
            return Files.find(Paths.get("."), 3, erBatchensJarfil())
                    .findAny()
                    .map(Path::toFile)
                    .map(File::getAbsolutePath)
                    .orElseThrow(OutOfProcessBatchRunner::klarteIkkjeLokalisereJarfila);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static IllegalArgumentException klarteIkkjeLokalisereJarfila() {
        return new IllegalArgumentException("Klarte ikkje lokalisere JAR-fila for pu-fak-ba-10 via søk frå rota av katalogen " +
                Paths.get(".").toFile().getAbsolutePath());
    }

    private static BiPredicate<Path, BasicFileAttributes> erBatchensJarfil() {
        return (path, ignorer) -> path.toFile().getName().startsWith("pu-fak-ba-10") &&
                path.toFile().getName().endsWith(".jar") &&
                !path.toFile().getName().contains("-shaded") &&
                !path.toFile().getName().contains("-tests") &&
                !path.toFile().getName().contains("-javadoc") &&
                !path.toFile().getName().contains("-sources");
    }

    private static AbstractCharSequenceAssert<?, String> assertContentOf(final File file) throws IOException {
        return assertThat(contentOf(file)).as(file.getName());
    }

    private static String contentOf(final File file) throws IOException {
        try (final Stream<String> stream = Files.lines(file.toPath(), Charset.defaultCharset())) {
            return stream.collect(joining("\n"));
        }
    }
}
