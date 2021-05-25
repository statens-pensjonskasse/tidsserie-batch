package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Streams.forEach;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.Test;

public class MeldingarTest {
    private final Meldingar meldingar = new Meldingar();

    @Test
    public void skal_emitte_antall_feil() {
        final RuntimeException e = new RuntimeException();
        forEach(
                IntStream.range(0, 1000).boxed(),
                index -> meldingar.emitError(e)
        );
        assertThat(
                meldingar.toMap()
        )
                .containsEntry("errors", 1000);
    }

    @Test
    public void skal_emitte_antall_pr_feiltype() {
        meldingar.emitError(new NullPointerException());
        meldingar.emitError(new IndexOutOfBoundsException());
        meldingar.emitError(new UnsupportedOperationException());
        meldingar.emitError(new IllegalStateException());

        assertThat(
                meldingar.toMap()
        )
                .containsEntry("errors_type_NullPointerException", 1)
                .containsEntry("errors_type_IndexOutOfBoundsException", 1)
                .containsEntry("errors_type_UnsupportedOperationException", 1)
                .containsEntry("errors_type_IllegalStateException", 1);
    }

    @Test
    public void skal_emitte_antall_pr_feilmelding() {
        meldingar.emitError(new RuntimeException("Wir sind falsch"));
        meldingar.emitError(new StackOverflowError("Der anfang ist das ende"));
        meldingar.emitError(new StackOverflowError("Das ende ist der anfang"));

        assertThat(
                meldingar.toMap()
        )
                .containsEntry("errors_message_Wir sind falsch", 1)
                .containsEntry("errors_message_Der anfang ist das ende", 1)
                .containsEntry("errors_message_Das ende ist der anfang", 1);
    }

    @Test
    public void skal_inkludere_meldingar_frå_begge() {
        final Meldingar forrige = new Meldingar();
        forrige.emit("A");
        forrige.emit("C");

        final Meldingar neste = new Meldingar();
        forrige.emit("A");
        forrige.emit("B");

        final Meldingar begge = forrige.merge(neste);
        assertThat(begge.toMap()).containsKeys("A", "B", "C");
    }

    @Test
    public void skal_legge_saman_antall_meldingar_pr_type_frå_begge() {
        final Meldingar forrige = new Meldingar();
        forrige.emit("A");
        forrige.emit("C");

        final Meldingar neste = new Meldingar();
        forrige.emit("A");
        forrige.emit("B");

        final Meldingar begge = forrige.merge(neste);
        assertThat(begge.toMap())
                .containsEntry("A", 2)
                .containsEntry("B", 1)
                .containsEntry("C", 1);
    }
}