package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;
import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.medlemsdata;
import static no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.spk.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;
import no.spk.pensjon.faktura.tjenesteregister.support.SimpleServiceRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartisjonertMedlemsdataBackendTest {
    private PartisjonertMedlemsdataBackend backend;

    private GenererTidsserieCommand kommando;

    private PartisjonertMedlemsdataOpplaster partisjonertOpplaster = new PartisjonertMedlemsdataOpplaster(new SimpleServiceRegistry());

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
                },
                new DefaultDatalagringStrategi(), partisjonertOpplaster);
    }

    @Test
    void skal_bevare_norske_tegn_i_medlemsdatane() {
        backend.put("ABCD", medlemsdata(rad("ÆØÅæøå")).medlemsdata(), new DefaultDatalagringStrategi());

        final Map<String, List<List<String>>> actual = klargjerFangingAvMedlemsdata();
        backend.lagTidsserie();

        assertThat(actual)
                .containsEntry("ABCD", Collections.singletonList(rad("ÆØÅæøå")));
    }

    @Test
    void skal_kalle_kommando_en_gang_pr_medlem() {
        backend.put("Donald", medlemsdata(rad("ABCD")).medlemsdata(), new DefaultDatalagringStrategi());
        backend.put("Dolly", medlemsdata(rad("1234")).medlemsdata(), new DefaultDatalagringStrategi());

        final Map<String, List<List<String>>> actual = klargjerFangingAvMedlemsdata();
        backend.lagTidsserie();

        assertThat(actual)
                .hasSize(2)
                .containsEntry("Donald", Collections.singletonList(rad("ABCD")))
                .containsEntry("Dolly", Collections.singletonList(rad("1234")));
    }

    @Test
    void skal_sluke_og_rapportere_alle_runtime_exceptions_frå_kommandoen() {
        backend.put("Martha", medlemsdata(rad("Ende", "Anfang", "ende")).medlemsdata(), new DefaultDatalagringStrategi());

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
        backend.put("Martha", medlemsdata(rad("Ende", "Anfang", "ende")).medlemsdata(), new DefaultDatalagringStrategi());

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
}