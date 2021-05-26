package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Partisjonsnummer.partisjonsnummer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

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

    void put(final String medlemsId, final Medlemsdata data) {
        partisjonar
                .get(partisjon(medlemsId))
                .put(medlemsId, data);
    }

    Optional<List<List<String>>> get(final String medlemsId) {
        return partisjonar
                .get(partisjon(medlemsId))
                .get(medlemsId);
    }

    private Partisjonsnummer partisjon(final String medlemsId) {
        final byte[] bytes = medlemsId.getBytes(StandardCharsets.UTF_8);
        final long hash = MurmurHash.hash64(
                bytes,
                bytes.length
        );
        final long index = abs(hash) % partisjonar.size();
        return partisjonsnummer(1 + index);
    }
}
