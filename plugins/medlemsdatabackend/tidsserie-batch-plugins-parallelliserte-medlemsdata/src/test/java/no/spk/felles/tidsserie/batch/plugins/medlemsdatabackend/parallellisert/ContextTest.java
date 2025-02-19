package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.Streams.forEach;
import static org.assertj.core.api.Assertions.assertThat;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

import org.junit.jupiter.api.Test;

class ContextTest {
    @Test
    void skal_foreløpig_basere_serienummer_på_partisjonsnummeret() {
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