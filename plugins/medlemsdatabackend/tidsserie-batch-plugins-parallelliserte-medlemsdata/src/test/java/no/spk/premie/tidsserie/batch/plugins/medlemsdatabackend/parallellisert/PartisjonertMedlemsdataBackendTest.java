package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.spk.premie.tidsserie.batch.core.medlem.GenererTidsserieCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartisjonertMedlemsdataBackendTest {
    private PartisjonertMedlemsdataBackend backend;

    private GenererTidsserieCommand kommando;

    @BeforeEach
    void _before() {
        backend = new PartisjonertMedlemsdataBackend(
                antallProsessorar(1),
                new KommandoKjoerer.SynkronKjoerer<>(),
                (serienummer, meldingar) -> {
                },
                (key, medlemsdata, context) ->
                        requireNonNull(kommando, "kommando er påkrevd, men var null")
                                .generer(key, medlemsdata, context),
                (medlemsId, t) -> {
                }
        );
    }

    @Test
    void skal_bevare_norske_tegn_i_medlemsdatane() {
        backend.put("ABCD", medlemsdata(rad("ÆØÅæøå")));

        final Map<String, List<List<String>>> actual = klargjerFangingAvMedlemsdata();
        backend.lagTidsserie();

        assertThat(actual)
                .containsEntry("ABCD", medlemsdata(rad("ÆØÅæøå")));
    }

    @Test
    void skal_kalle_kommando_en_gang_pr_medlem() {
        backend.put("Donald", medlemsdata(rad("ABCD")));
        backend.put("Dolly", medlemsdata(rad("1234")));

        final Map<String, List<List<String>>> actual = klargjerFangingAvMedlemsdata();
        backend.lagTidsserie();

        assertThat(actual)
                .hasSize(2)
                .containsEntry("Donald", medlemsdata(rad("ABCD")))
                .containsEntry("Dolly", medlemsdata(rad("1234")));
    }

    @Test
    void skal_sluke_og_rapportere_alle_runtime_exceptions_frå_kommandoen() {
        backend.put("Martha", medlemsdata(rad("Ende", "Anfang", "ende")));

        registrerKommando(
                (key, medlemsdata, tidsserieContext) -> {
                    throw new IllegalStateException("Hei");
                }
        );

        final Map<String, Integer> actual = backend.lagTidsserie();
        assertThat(actual)
                .hasSize(4)
                .containsEntry("errors", 1)
                .containsEntry("errors_message_Hei", 1)
                .containsEntry("errors_type_IllegalStateException", 1)
                .containsEntry("medlem", 1);
    }

    @Test
    void skal_sluke_og_rapportere_alle_errors_frå_kommandoen() {
        backend.put("Martha", medlemsdata(rad("Ende", "Anfang", "ende")));

        registrerKommando(
                (key, medlemsdata, tidsserieContext) -> {
                    throw new OutOfMemoryError("Hei");
                }
        );

        final Map<String, Integer> actual = backend.lagTidsserie();
        assertThat(actual)
                .hasSize(4)
                .containsEntry("errors", 1)
                .containsEntry("errors_message_Hei", 1)
                .containsEntry("errors_type_OutOfMemoryError", 1)
                .containsEntry("medlem", 1);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static List<List<String>> medlemsdata(final List<String>... rader) {
        return Arrays.asList(rader);
    }

    private Map<String, List<List<String>>> klargjerFangingAvMedlemsdata() {
        final Map<String, List<List<String>>> actual = new HashMap<>();
        registrerKommando(huskMedlemsdatane(actual));
        return actual;
    }

    private void registrerKommando(final GenererTidsserieCommand kommando) {
        this.kommando = requireNonNull(kommando, "kommando er påkrevd, men var null");
    }

    private GenererTidsserieCommand huskMedlemsdatane(final Map<String, List<List<String>>> actual) {
        return (key, medlemsdata, tidsserieContext) -> actual.put(key, medlemsdata);
    }

    private static List<String> rad(final String... celle) {
        return Arrays.asList(celle);
    }
}