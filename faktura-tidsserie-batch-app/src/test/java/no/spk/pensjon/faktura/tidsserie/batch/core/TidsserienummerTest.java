package no.spk.pensjon.faktura.tidsserie.batch.core;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer.genererForDato;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TidsserienummerTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void skalGenerereTidsserienummerBasertDatoen() {
        assertTidsserienummer(dato("1917.01.01")).isEqualTo("19170101");
        assertTidsserienummer(dato("2015.11.08")).isEqualTo("20151108");
        assertTidsserienummer(LocalDate.of(1, 1, 1)).isEqualTo("00010101");
        assertTidsserienummer(LocalDate.of(9999, 12, 31)).isEqualTo("99991231");
    }

    @Test
    public void skalIkkjeGodtaNummerLengreEnn8Siffer() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("må vere 8-siffer");
        e.expectMessage("var 10-siffer");
        e.expectMessage("(+999990101)");
        Tidsserienummer.genererForDato(LocalDate.of(99999, 1, 1));
    }

    private static AbstractCharSequenceAssert<?, String> assertTidsserienummer(LocalDate dato) {
        return assertThat(genererForDato(dato).toString()).as("tidsserienummer for dato " + dato);
    }
}