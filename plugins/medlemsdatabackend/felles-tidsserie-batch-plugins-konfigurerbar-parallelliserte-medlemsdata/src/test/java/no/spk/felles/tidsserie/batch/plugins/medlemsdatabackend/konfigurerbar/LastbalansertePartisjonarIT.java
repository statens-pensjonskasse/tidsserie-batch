package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Nodenummer.nodenummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.pensjon.faktura.tjenesteregister.support.SimpleServiceRegistry;

import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LastbalansertePartisjonarIT {

    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    private final Partisjonstabell partisjonstabell = new Partisjonstabell();

    private final CompositePartisjonListener partisjonListeners = (serienummer, meldingar) -> {
    };

    private final PartisjonertMedlemsdataOpplaster partisjonertOpplaster = new PartisjonertMedlemsdataOpplaster(new SimpleServiceRegistry());

    @Test
    void skal_lastbalansere_partisjonar_basert_på_nodenummer_og_partisjonsnummer() {
        assertPartisjonarPrNode(
                byggNoder(1)
        )
                .hasSize(1)
                .hasEntrySatisfying(nodenummer(1, 1), partisjonar -> verifiserPartisjonar(1, partisjonar, 271, 0));

        assertPartisjonarPrNode(
                byggNoder(2)
        )
                .hasSize(2)
                .hasEntrySatisfying(nodenummer(1, 2), partisjonar -> verifiserPartisjonar(2, partisjonar, 136, 0))
                .hasEntrySatisfying(nodenummer(2, 2), partisjonar -> verifiserPartisjonar(2, partisjonar, 135, 1));

        assertPartisjonarPrNode(
                byggNoder(3)
        )
                .hasSize(3)
                .hasEntrySatisfying(nodenummer(1, 3), partisjonar -> verifiserPartisjonar(3, partisjonar, 91, 0))
                .hasEntrySatisfying(nodenummer(2, 3), partisjonar -> verifiserPartisjonar(3, partisjonar, 90, 1))
                .hasEntrySatisfying(nodenummer(3, 3), partisjonar -> verifiserPartisjonar(3, partisjonar, 90, 2))
        ;

        assertPartisjonarPrNode(
                byggNoder(8)
        )
                .hasSize(8)
                .hasEntrySatisfying(nodenummer(1, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 0))
                .hasEntrySatisfying(nodenummer(2, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 1))
                .hasEntrySatisfying(nodenummer(3, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 2))
                .hasEntrySatisfying(nodenummer(4, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 3))
                .hasEntrySatisfying(nodenummer(5, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 4))
                .hasEntrySatisfying(nodenummer(6, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 5))
                .hasEntrySatisfying(nodenummer(7, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 1, 6))
                .hasEntrySatisfying(nodenummer(8, 8), partisjonar -> verifiserPartisjonar(8, partisjonar, 271 / 8 + 0, 7))
        ;
    }

    @Test
    void skal_starte_prosessering_av_alle_noder_for_å_unngå_at_nodene_blir_behandla_sekvensielt_etterkvart_som_ein_itererer_over_resultata() {
        final LastbalansertePartisjonar lastbalansering = lastbalanser(byggNoder(16));

        IntStream.rangeClosed(1, 16).forEach(
                limit -> {
                    final KommandoKjoerer.Spion<Meldingar> spion = new KommandoKjoerer.Spion<>();

                    lastbalansering
                            .startParallellprosessering(
                                    spion,
                                    (medlemsId, medlemsdata, context) -> {
                                    },
                                    partisjonListeners,
                                    (medlemsId, t) -> {
                                    },
                                    partisjonertOpplaster)
                            .limit(limit)
                            .toList();

                    assertThat(spion.tasks()).hasSize(16);
                }
        );
    }

    private MapAssert<Nodenummer, Set<Partisjonsnummer>> assertPartisjonarPrNode(final Stream<Nodenummer> noder) {
        return assertThat(
                lastbalanser(noder)
                        .partisjonarPrNode()
        );
    }

    private LastbalansertePartisjonar lastbalanser(final Stream<Nodenummer> noder) {
        return LastbalansertePartisjonar.lastbalanser(
                partisjonstabell,
                noder
        );
    }

    private Stream<Nodenummer> byggNoder(final int antallNoder) {
        return
                IntStream
                        .rangeClosed(1, antallNoder)
                        .mapToObj(
                                node -> nodenummer(node, antallNoder)
                        );
    }

    private ListAssert<Partisjonsnummer> verifiserPartisjonar(
            final int antallNoder,
            final Set<Partisjonsnummer> partisjonar,
            final int forventaAntallPartisjonar,
            final int fordelingsnøkkel
    ) {
        return assertThat(partisjonar.stream())
                .hasSize(forventaAntallPartisjonar)
                .allMatch(partisjonsnummer -> partisjonsnummer.index() % antallNoder == fordelingsnøkkel);
    }
}
