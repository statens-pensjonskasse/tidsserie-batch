package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Streams.forEach;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ContextTest {
    @Test
    public void skal_foreløpig_basere_serienummer_på_partisjonsnummeret() {
        forEach(
                Partisjonsnummer.stream(),
                partisjonsnummer ->
                        assertThat(
                                new Context(partisjonsnummer)
                        )
                                .satisfies(
                                        context ->
                                                assertThat(context.getSerienummer())
                                                        .as("<%s>.serienummer()", context)
                                                        .isEqualTo(partisjonsnummer.index() + 1)
                                )
        );
    }
}