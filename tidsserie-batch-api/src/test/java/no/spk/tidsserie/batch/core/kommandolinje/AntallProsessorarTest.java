package no.spk.tidsserie.batch.core.kommandolinje;

import static java.util.stream.IntStream.rangeClosed;
import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;
import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.standardAntallProsessorar;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.assertj.core.api.ListAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(SoftAssertionsExtension.class)
public class AntallProsessorarTest {

    @RegisterExtension
    private final AvailableProcessors prosessorar = new AvailableProcessors();

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void skal_takle_maskiner_som_har_1_kjerne() {
        prosessorar.overstyr(1);
        softly.assertThat(
                standardAntallProsessorar()
        )
                .isEqualTo(antallProsessorar(1));
    }

    @Test
    void skal_ikkje_bruke_alle_prosessorar_som_standard() {
        rangeClosed(2, 1000)
                .peek(prosessorar::overstyr)
                .map(antall -> antall - 1)
                .forEach(
                        expected -> softly.assertThat(
                                standardAntallProsessorar()
                        )
                                .isEqualTo(
                                        antallProsessorar(expected)
                                )
                );
    }

    @Test
    void skal_hente_antall_tilgjengelige_prosessorar_via_JVMen() {
        assertThat(availableProcessors())
                .isEqualTo(antallProsessorar(Runtime.getRuntime().availableProcessors()));
    }

    @Test
    void skal_ikkje_tillate_verdiar_mindre_enn_1() {
        Stream.of(0, -1, Integer.MIN_VALUE).forEach(
                antall -> softly
                        .assertThatCode(() -> antallProsessorar(0))
                        .isInstanceOf(IllegalArgumentException.class)
        );
    }

    @Test
    void skal_godta_verdiar_frå_1_og_oppover() {
        Stream.of(1, 2, 3, 4).forEach(
                antall -> softly
                        .assertThat(antallProsessorar(antall))
                        .hasToString("antall prosessorar " + antall)
        );
    }

    @Test
    void skal_tillate_verdiar_over_antall_kjerner_på_maskina() {
        final int veldigVeldigMange = Runtime.getRuntime().availableProcessors() * 1024;
        softly
                .assertThat(antallProsessorar(veldigVeldigMange))
                .hasToString("antall prosessorar " + veldigVeldigMange);
    }

    @Test
    void skal_generere_ein_range_frå_1_til_antall_prosessorar() {
        assertStream(antallProsessorar(1)).hasSize(1).containsExactlyElementsOf(range("1->1"));
        assertStream(antallProsessorar(2)).hasSize(2).containsExactlyElementsOf(range("1->2"));
        assertStream(antallProsessorar(3)).hasSize(3).containsExactlyElementsOf(range("1->3"));
        assertStream(antallProsessorar(4)).hasSize(4).containsExactlyElementsOf(range("1->4"));
        assertStream(antallProsessorar(8)).hasSize(8).containsExactlyElementsOf(range("1->8"));
        assertStream(antallProsessorar(16)).hasSize(16).containsExactlyElementsOf(range("1->16"));
    }

    @Test
    void skal_ha_en_getter_for_underliggende_verdi() {
        assertThat(antallProsessorar(4).antall()).isEqualTo(4);
        assertThat(antallProsessorar(14).antall()).isEqualTo(14);
        assertThat(antallProsessorar(20).antall()).isEqualTo(20);
    }

    private ListAssert<Integer> assertStream(final AntallProsessorar antall) {
        return softly.assertThat(
                antall
                        .stream()
                        .boxed()
                        .toList()
        )
                .as("(%s).stream()", antall);
    }

    private static Iterable<Integer> range(final String expected) {
        final int første = Integer.parseInt(expected.split("->")[0]);
        final int siste = Integer.parseInt(expected.split("->")[1]);
        return rangeClosed(første, siste)
                .boxed()
                .toList();
    }
}
