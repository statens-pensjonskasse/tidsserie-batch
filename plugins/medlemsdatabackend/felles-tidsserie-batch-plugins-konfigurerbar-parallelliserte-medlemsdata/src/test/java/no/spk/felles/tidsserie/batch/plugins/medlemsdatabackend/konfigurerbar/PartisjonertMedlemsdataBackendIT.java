package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.felles.tidsserie.batch.core.medlem.PartisjonsListener;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;
import no.spk.pensjon.faktura.tjenesteregister.support.SimpleServiceRegistry;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartisjonertMedlemsdataBackendIT {
    private PartisjonertMedlemsdataBackend backend;

    private GenererTidsserieCommand kommando;

    private PartisjonsListener partisjonsListener = serienummer -> {
    };

    private PartisjonertMedlemsdataOpplaster partisjonertOpplaster = new PartisjonertMedlemsdataOpplaster(new SimpleServiceRegistry());

    @BeforeEach
    void _before() {
        backend = new PartisjonertMedlemsdataBackend(
                antallProsessorar(1),
                KommandoKjoerer.velgFlertrådskjøring(1),
                (serienummer, meldingar) ->
                        requireNonNull(partisjonsListener, "partisjonsListener er påkrevd, men var null")
                                .partitionInitialized(serienummer.index() + 1),
                (medlemsId, medlemsdata, context) ->
                        requireNonNull(kommando, "kommando er påkrevd, men var null")
                                .generer(medlemsId, medlemsdata, context),
                (medlemsId, t) -> {
                },
                new DefaultDatalagringStrategi(), partisjonertOpplaster);
    }

    @Test
    void skal_gjere_alle_medlemsdata_lasta_opp_til_backenden_tilgjengelig_for_kommandoen() {
        upload(
                backend.uploader(),
                "Martha A;FØDT;2003",
                "Martha A;DØD;2020.06.27",
                "Jonas B;FØDT;2002",
                "Jonas B;DØD;2019.11.07"
        );

        final Map<String, List<List<String>>> medlemsdata = klargjerFangingAvMedlemsdata();
        assertThat(
                backend.lagTidsserie()
        )
                .containsEntry("medlem", 2);

        assertThat(medlemsdata)
                .containsEntry(
                        "Martha A",
                        Lists.newArrayList(
                                rad("FØDT", "2003"),
                                rad("DØD", "2020.06.27")
                        )
                )
                .containsEntry(
                        "Jonas B",
                        Lists.newArrayList(
                                rad("FØDT", "2002"),
                                rad("DØD", "2019.11.07")
                        )
                );
    }

    @Test
    void skal_ta_vare_på_alle_medlemsdatane_for_et_medlem_sjølv_for_uttrekk_som_ikkje_er_sortert_på_medlemsid() {
        upload(
                backend.uploader(),
                "Martha A;DØD;2020.06.27",
                "Jonas B;DØD;2019.11.07",
                "Martha A;FØDT;2003",
                "Jonas B;FØDT;2002"
        );

        final Map<String, List<List<String>>> medlemsdata = klargjerFangingAvMedlemsdata();
        assertThat(
                backend.lagTidsserie()
        )
                .containsEntry("medlem", 2);

        assertThat(medlemsdata)
                .containsEntry(
                        "Martha A",
                        Lists.newArrayList(
                                rad("DØD", "2020.06.27"),
                                rad("FØDT", "2003")
                        )
                )
                .containsEntry(
                        "Jonas B",
                        Lists.newArrayList(
                                rad("DØD", "2019.11.07"),
                                rad("FØDT", "2002")
                        )
                );
    }

    @Test
    void skal_notifisere_lyttarane_kvar_gang_behandling_av_ein_ny_partisjon_startar() {
        final Map<Long, Boolean> partisjonInitialisert = new ConcurrentHashMap<>();

        partisjonsListener = serienummer -> partisjonInitialisert.put(serienummer, true);

        backend.lagTidsserie();

        assertThat(partisjonInitialisert).hasSize(271);
    }

    @Test
    void skal_notifisere_partisjonslyttarane_frå_samme_tråd_som_partisjonen_blir_behandla_av() {
        final Map<Long, String> partisjonInitialisert = new ConcurrentHashMap<>();

        partisjonsListener = serienummer -> partisjonInitialisert.put(serienummer, Thread.currentThread().getName());

        backend.lagTidsserie();

        assertThat(partisjonInitialisert).hasSize(271);
        assertThat(partisjonInitialisert.values()).allMatch("pa-res-ba-01-1"::equals);
    }

    private void upload(final MedlemsdataUploader uploader, final String... linjer) {
        Stream.of(linjer)
                .map(linje -> linje.split(";", -1))
                .map(Arrays::asList)
                .map(Medlemslinje::new)
                .forEach(
                        linje -> {
                            uploader.append(linje);
                            uploader.run();
                        }
                );
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