package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * Integrasjonstestar for {@link FileTemplate}.
 *
 * @author Tarjei Skorgenes
 */
public class FileTemplateIT {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final TestName name = new TestName();

    private FileTemplate template;

    @Before
    public void _before() {
        template = new FileTemplate(
                temp.getRoot().toPath(),
                prefix(),
                suffix()
        );
    }

    @Test
    public void skalGenerereForskjelligeFilnavnForKvartKall() {
        assertFilename(1L)
                .isNotEqualTo(
                        create(1L).getName()
                );
    }

    @Test
    public void skalInkludereSerienummerIKvartFilnavn() {
        assertFilename(1762)
                .startsWith(prefix() + "1762-");
    }

    private AbstractCharSequenceAssert<?, String> assertFilename(final long serienummer) {
        final File file = create(serienummer);
        return assertThat(file.getName())
                .as("filnavn for " + file);
    }

    private File create(final long serienummer) {
        return template.createUniqueFile(serienummer);
    }

    private String suffix() {
        return "-test";
    }

    private String prefix() {
        return FileTemplateIT.class.getSimpleName() + "-" + name.getMethodName() + "-";
    }
}