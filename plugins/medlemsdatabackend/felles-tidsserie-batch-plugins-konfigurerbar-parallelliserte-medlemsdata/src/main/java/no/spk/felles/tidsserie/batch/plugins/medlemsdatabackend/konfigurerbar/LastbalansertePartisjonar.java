package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.ProsesserNode.AsyncResultat;

class LastbalansertePartisjonar {
    private final Map<Nodenummer, Set<Partisjon>> lastbalansering;

    private LastbalansertePartisjonar(final Map<Nodenummer, Set<Partisjon>> lastbalansering) {
        this.lastbalansering = requireNonNull(lastbalansering, "lastbalansering er påkrevd, men var null");
    }

    static LastbalansertePartisjonar lastbalanser(final Partisjonstabell partisjonstabell, final Stream<Nodenummer> noder) {
        return new LastbalansertePartisjonar(
                noder
                        .collect(
                                toMap(
                                        Function.identity(),
                                        partisjonstabell::partisjonarFor
                                )
                        )
        );
    }

    Stream<AsyncResultat> startParallellprosessering(
            final KommandoKjoerer<Meldingar> executor,
            final GenererTidsserieCommand kommando,
            final CompositePartisjonListener partisjonsListeners,
            final MedlemFeilarListener medlemFeilarListener
    ) {
        return
                lastbalansering
                        .values()
                        .stream()
                        .map(
                                partisjonar -> new ProsesserNode(
                                        partisjonar,
                                        kommando,
                                        partisjonsListeners,
                                        medlemFeilarListener
                                )
                        )
                        .map(node -> node.start(executor))
                        .toList()
                        .stream()
                ;
    }

    // Kun for testing
    Map<Nodenummer, Set<Partisjonsnummer>> partisjonarPrNode() {
        final Map<Nodenummer, Set<Partisjonsnummer>> tmp = new HashMap<>();
        lastbalansering.forEach(
                (nodenummer, partisjonar) -> tmp.put(
                        nodenummer,
                        partisjonar
                                .stream()
                                .map(Partisjon::nummer)
                                .collect(toSet())
                )
        );
        return tmp;
    }
}
