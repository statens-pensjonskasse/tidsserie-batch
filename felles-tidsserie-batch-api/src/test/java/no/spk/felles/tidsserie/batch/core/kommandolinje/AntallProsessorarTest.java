package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;

import java.util.stream.Stream;

import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.ProxyableListAssert;
import org.junit.Rule;
import org.junit.Test;

public class AntallProsessorarTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void skal_ikkje_tillate_verdiar_mindre_enn_1() {
        Stream.of(0, -1, Integer.MIN_VALUE).forEach(
                antall -> softly
                        .assertThatCode(() -> antallProsessorar(0))
                        .isInstanceOf(IllegalArgumentException.class)
        );
    }

    @Test
    public void skal_godta_verdiar_frå_1_og_oppover() {
        Stream.of(1, 2, 3, 4).forEach(
                antall -> softly
                        .assertThat(antallProsessorar(antall))
                        .hasToString("antall prosessorar " + antall)
        );
    }

    @Test
    public void skal_tillate_verdiar_over_antall_kjerner_på_maskina() {
        final int veldigVeldigMange = Runtime.getRuntime().availableProcessors() * 1024;
        softly
                .assertThat(antallProsessorar(veldigVeldigMange))
                .hasToString("antall prosessorar " + veldigVeldigMange);
    }

    @Test
    public void skal_generere_ein_range_frå_1_til_antall_prosessorar() {
        assertStream(antallProsessorar(1)).hasSize(1).containsExactlyElementsOf(range("1->1"));
        assertStream(antallProsessorar(2)).hasSize(2).containsExactlyElementsOf(range("1->2"));
        assertStream(antallProsessorar(3)).hasSize(3).containsExactlyElementsOf(range("1->3"));
        assertStream(antallProsessorar(4)).hasSize(4).containsExactlyElementsOf(range("1->4"));
        assertStream(antallProsessorar(8)).hasSize(8).containsExactlyElementsOf(range("1->8"));
        assertStream(antallProsessorar(16)).hasSize(16).containsExactlyElementsOf(range("1->16"));
    }

    private ProxyableListAssert<Integer> assertStream(final AntallProsessorar antall) {
        return softly.assertThat(
                antall
                        .stream()
                        .boxed()
                        .collect(toList())
        )
                .as("(%s).stream()", antall);
    }

    private static Iterable<Integer> range(final String expected) {
        final int første = Integer.parseInt(expected.split("->")[0]);
        final int siste = Integer.parseInt(expected.split("->")[1]);
        return rangeClosed(første, siste)
                .boxed().collect(toList());
    }
}