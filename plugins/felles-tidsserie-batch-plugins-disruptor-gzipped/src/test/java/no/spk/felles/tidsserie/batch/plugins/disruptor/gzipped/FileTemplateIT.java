package no.spk.felles.tidsserie.batch.plugins.disruptor.gzipped;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Optional;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integrasjonstestar for {@link FileTemplate}.
 *
 * @author Tarjei Skorgenes
 */
public class FileTemplateIT {
    @TempDir
    public File temp;

    
    private String name;

    private FileTemplate template;

    @BeforeEach
    void _before(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        testMethod.ifPresentOrElse(method -> this.name = method.getName(), () -> this.name = "ukjent-metode");
        template = new FileTemplate(
                temp.toPath(),
                suffix()
        );
    }

    @Test
    void skalGenerereForskjelligeFilnavnForKvartKall() {
        assertFilename(1L)
                .isNotEqualTo(
                        create(1L).getName()
                );
    }

    @Test
    void skalInkludereSerienummerIKvartFilnavn() {
        assertFilename(1762)
                .startsWith(prefix() + "1762-");
    }

    private AbstractCharSequenceAssert<?, String> assertFilename(final long serienummer) {
        final File file = create(serienummer);
        return assertThat(file.getName())
                .as("filnavn for " + file);
    }

    private File create(final long serienummer) {
        return template.createUniqueFile(serienummer, prefix());
    }

    private String suffix() {
        return "-test";
    }

    private String prefix() {
        return FileTemplateIT.class.getSimpleName() + "-" + name + "-";
    }
}