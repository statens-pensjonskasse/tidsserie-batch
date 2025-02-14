package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.Nodenummer.nodenummer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class NodenummerTest {
    @Test
    void skal_handheve_at_antall_noder_er_større_enn_eller_lik_1() {
        assertThatCode(() -> nodenummer(1, 0)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> nodenummer(1, 1)).doesNotThrowAnyException();
    }

    @Test
    void skal_handheve_at_nodenummer_ikkje_kan_vere_mindre_enn_1() {
        assertThatCode(() -> nodenummer(-1, 1)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> nodenummer(0, 1)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> nodenummer(1, 1)).doesNotThrowAnyException();
    }

    @Test
    void skal_handheve_at_nodenummer_ikkje_kan_vere_større_enn_antall_noder() {
        assertThatCode(() -> nodenummer(4, 4)).doesNotThrowAnyException();
        assertThatCode(() -> nodenummer(5, 4)).isInstanceOf(AssertionError.class);
        assertThatCode(() -> nodenummer(6, 4)).isInstanceOf(AssertionError.class);
    }

    @Test
    void skal_kun_vere_lik_ved_samme_index_og_antall_noder() {
        assertThat(nodenummer(1, 4))
                .isEqualTo(nodenummer(1, 4))
                .isNotEqualTo(nodenummer(1, 5))
                .isNotEqualTo(null);
    }

    @Test
    void skal_ha_stabil_hashcode() {
        final Nodenummer nodenummer = nodenummer(1, 4);
        assertThat(nodenummer)
                .satisfies(actual -> assertThat(actual.hashCode()).isEqualTo(actual.hashCode()))
                .hasSameHashCodeAs(nodenummer(1, 4));
    }

    @Test
    void skal_ha_menneskevennlig_toString_med_index_erstatta_av_nodenummer() {
        assertThat(nodenummer(1, 4)).hasToString("node 1 av 4");
        assertThat(nodenummer(4, 4)).hasToString("node 4 av 4");
    }
}