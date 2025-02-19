package no.spk.premie.tidsserie.batch.core.grunnlagsdata;

import static no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer.partisjonsnummer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PartisjonsnummerTest {
    @Test
    void skal_handheve_at_index_ikkje_kan_vere_negativ() {
        assertThatCode(() -> partisjonsnummer(-1)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> partisjonsnummer(0)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> partisjonsnummer(1)).doesNotThrowAnyException();
    }

    @Test
    void skal_handheve_at_det_maksimalt_er_271_partisjonar() {
        assertThatCode(() -> partisjonsnummer(271)).doesNotThrowAnyException();
        assertThatCode(() -> partisjonsnummer(272)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> partisjonsnummer(273)).isInstanceOf(AssertionError.class);
    }

    @Test
    void skal_produsere_alle_mulige_partisjonsnummer() {
        assertThat(
                Partisjonsnummer.stream()
        )
                .containsExactlyElementsOf(
                        IntStream
                                .rangeClosed(1, 271)
                                .mapToObj(Partisjonsnummer::partisjonsnummer)
                                .toList()
                );
    }

    @Test
    void skal_kun_vere_lik_ved_samme_index() {
        assertThat(partisjonsnummer(1))
                .satisfies(self -> assertThat(self).isEqualTo(self))
                .isNotEqualTo(null)
                .isNotEqualTo(1L)
                .isEqualTo(partisjonsnummer(1))
        ;
    }

    @Test
    void skal_ha_stabil_hashcode() {
        assertThat(partisjonsnummer(1))
                .satisfies(actual -> assertThat(actual.hashCode()).isEqualTo(actual.hashCode()))
                .hasSameHashCodeAs(partisjonsnummer(1));
    }

    @Test
    void skal_ha_menneskevennlig_toString() {
        assertThat(partisjonsnummer(1)).hasToString("partisjon 1 av 271");
        assertThat(partisjonsnummer(271)).hasToString("partisjon 271 av 271");
    }
}
