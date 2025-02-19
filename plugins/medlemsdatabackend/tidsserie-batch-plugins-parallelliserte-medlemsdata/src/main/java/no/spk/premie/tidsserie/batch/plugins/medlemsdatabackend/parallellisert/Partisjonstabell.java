package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer.tilhørendePartisjonForMedlem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

class Partisjonstabell {
    private final Map<Partisjonsnummer, Partisjon> partisjonar;

    Partisjonstabell() {
        this.partisjonar =
                Partisjonsnummer
                        .stream()
                        .map(Partisjon::new)
                        .collect(
                                toMap(
                                        Partisjon::nummer,
                                        Function.identity()
                                )
                        );
    }

    void clear() {
        partisjonar.clear();
    }

    Set<Partisjon> partisjonarFor(final Nodenummer node) {
        return
                partisjonar
                        .keySet()
                        .stream()
                        .filter(node::skalHandtere)
                        .map(partisjonar::get)
                        .collect(toSet());
    }

    void put(final String medlemsId, final List<List<String>> data) {
        partisjonar
                .get(tilhørendePartisjonForMedlem(medlemsId))
                .put(medlemsId, data);
    }

    Optional<List<List<String>>> get(final String medlemsId) {
        return partisjonar
                .get(tilhørendePartisjonForMedlem(medlemsId))
                .get(medlemsId);
    }
}
