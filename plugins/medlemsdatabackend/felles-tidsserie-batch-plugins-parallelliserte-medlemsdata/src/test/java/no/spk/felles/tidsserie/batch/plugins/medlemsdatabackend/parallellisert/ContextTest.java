package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.Partisjonsnummer.partisjonsnummer;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.Streams.forEach;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.Test;

public class ContextTest {
    private final Context context = new Context(partisjonsnummer(1));

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

    @Test
    public void skal_emitte_antall_feil() {
        final RuntimeException e = new RuntimeException();
        forEach(
                IntStream.range(0, 1000).boxed(),
                index -> context.emitError(e)
        );
        assertThat(
                context.toMap()
        )
                .containsEntry("errors", 1000);
    }

    @Test
    public void skal_emitte_antall_pr_feiltype() {
        context.emitError(new NullPointerException());
        context.emitError(new IndexOutOfBoundsException());
        context.emitError(new UnsupportedOperationException());
        context.emitError(new IllegalStateException());

        assertThat(
                context.toMap()
        )
                .containsEntry("errors_type_NullPointerException", 1)
                .containsEntry("errors_type_IndexOutOfBoundsException", 1)
                .containsEntry("errors_type_UnsupportedOperationException", 1)
                .containsEntry("errors_type_IllegalStateException", 1);
    }

    @Test
    public void skal_emitte_antall_pr_feilmelding() {
        context.emitError(new RuntimeException("Wir sind falsch"));
        context.emitError(new StackOverflowError("Der anfang ist das ende"));
        context.emitError(new StackOverflowError("Das ende ist der anfang"));

        assertThat(
                context.toMap()
        )
                .containsEntry("errors_message_Wir sind falsch", 1)
                .containsEntry("errors_message_Der anfang ist das ende", 1)
                .containsEntry("errors_message_Das ende ist der anfang", 1);
    }
}