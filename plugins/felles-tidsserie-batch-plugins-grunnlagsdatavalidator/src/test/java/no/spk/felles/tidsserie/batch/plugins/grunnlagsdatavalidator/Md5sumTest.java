package no.spk.felles.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Md5sumTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private final Md5sum md5sum = new Md5sum();

    @Test
    public void skal_produsere_korrekt_md5sum() throws IOException {
        assertProduser("test.txt", "Litt innhold, inkludert ÆØÅ").isEqualTo("414b829c907168364b8e9ebe19da4455");
        assertProduser("test2.txt", "En vakker dag\nSa trollet:\nHei\n").isEqualTo("b5c0e69972d97abb98ad57571a0ecff1");
    }

    private AbstractCharSequenceAssert<?, String> assertProduser(final String filename, final String content) throws IOException {
        return assertThat(
                md5sum.produser(
                        write(filename, content)
                )
        );
    }

    private File write(final String filename, final String content) throws IOException {
        return Files
                .write(temp.newFile(filename).toPath(), content.getBytes(UTF_8))
                .toFile();
    }
}