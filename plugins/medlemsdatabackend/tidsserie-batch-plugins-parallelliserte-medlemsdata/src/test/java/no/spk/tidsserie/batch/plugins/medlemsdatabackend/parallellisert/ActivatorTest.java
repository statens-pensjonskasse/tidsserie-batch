package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static org.assertj.core.api.Assertions.assertThat;

import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.tidsserie.batch.core.registry.Ranking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ActivatorTest {

    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    @Test
    void skal_registrere_medlemsdatafeilerlistener_for_å_logge_medlemmar_som_feilar_nokonlunde_likt_slik_hazelcast_backenden_gjer_det() {
        medArgumenter(antallProsessorar(1));

        aktiver();

        registry
                .assertFirstService(
                        MedlemFeilarListener.class,
                        actual -> actual.isInstanceOf(Activator.MedlemFeilarLogger.class)
                );
    }

    @Test
    void skal_registrere_medlemsdatabackend_med_standard_ranking() {
        medArgumenter(antallProsessorar(1));

        aktiver();

        registry
                .assertFirstService(
                        MedlemsdataBackend.class,
                        actual ->
                                actual
                                        .harRanking(Ranking.standardRanking())
                                        .isInstanceOf(PartisjonertMedlemsdataBackend.class)
                );
    }

    @Test
    void skal_plugge_backenden_inn_i_tidsserie_livssyklusen_for_å_kunne_starte_og_stoppe() {
        medArgumenter(antallProsessorar(1));

        aktiver();

        registry
                .assertAllServices(TidsserieLivssyklus.class)
                .hasSize(1)
                .allSatisfy(
                        actual -> assertThat(actual).isInstanceOf(PartisjonertMedlemsdataBackend.class)
                );
    }

    @Test
    void skal_sette_opp_backend_med_antall_trådar_likt_antall_prosessorar_angitt_ved_oppstart_av_batch() {
        final AntallProsessorar expected = antallProsessorar(137);
        medArgumenter(expected);

        aktiver();

        registry
                .assertFirstService(
                        MedlemsdataBackend.class,
                        actual ->
                                actual
                                        .som(PartisjonertMedlemsdataBackend.class)
                                        .satisfies(
                                                backend ->
                                                        assertThat(backend.antallProsessorar())
                                                                .isEqualTo(expected)
                                        )
                );
    }

    private void aktiver() {
        new Activator().aktiver(registry.registry());
    }

    private void medArgumenter(final AntallProsessorar antall) {
        registry.registrer(
                TidsserieBatchArgumenter.class,
                ArgumenterStub.medAntallProsessorar(antall)
        );
    }
}