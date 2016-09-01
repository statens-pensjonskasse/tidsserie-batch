package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ContextTest {

    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void skal_akkumulere_naar_gyldige_tellere() {
        Context context = new Context();
        context.emit("error 1", 1);
        context.emit("error 1", 1);
        context.emit("error 2", 1);
        assertThat(context.resultat()).containsKey("error 1");
        assertThat(context.resultat()).containsKey("error 2");
        assertThat(context.resultat()).containsEntry("error 1", 2);
        assertThat(context.resultat()).containsEntry("error 2", 1);
    }

    @Test
    public void skal_feile_naar_ugyldig_value_0() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("telleren kan ikke være mindre enn 1");
        Context context = new Context();
        context.emit("error", 0);
    }

    @Test
    public void skal_feile_naar_ugyldig_value_null() {
        e.expect(NullPointerException.class);
        e.expectMessage("teller er påkrevd, men var null");
        Context context = new Context();
        context.emit("error", null);
    }
}