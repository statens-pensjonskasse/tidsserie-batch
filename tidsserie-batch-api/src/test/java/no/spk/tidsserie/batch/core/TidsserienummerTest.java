package no.spk.tidsserie.batch.core;

import static no.spk.tidsserie.batch.core.Datoar.dato;
import static no.spk.tidsserie.batch.core.Tidsserienummer.genererForDato;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.Test;

class TidsserienummerTest {

    @Test
    void skalGenerereTidsserienummerBasertDatoen() {
        assertTidsserienummer(dato("1917.01.01")).isEqualTo("19170101");
        assertTidsserienummer(dato("2015.11.08")).isEqualTo("20151108");
        assertTidsserienummer(LocalDate.of(1, 1, 1)).isEqualTo("00010101");
        assertTidsserienummer(LocalDate.of(9999, 12, 31)).isEqualTo("99991231");
    }

    @Test
    void skalIkkjeGodtaNummerLengreEnn8Siffer() {
        assertThatCode(
                () -> Tidsserienummer.genererForDato(LocalDate.of(99999, 1, 1))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("må vere 8-siffer")
                .hasMessageContaining("var 10-siffer")
                .hasMessageContaining("(+999990101)")
        ;
    }

    private static AbstractCharSequenceAssert<?, String> assertTidsserienummer(LocalDate dato) {
        return assertThat(genererForDato(dato).toString()).as("tidsserienummer for dato " + dato);
    }
}