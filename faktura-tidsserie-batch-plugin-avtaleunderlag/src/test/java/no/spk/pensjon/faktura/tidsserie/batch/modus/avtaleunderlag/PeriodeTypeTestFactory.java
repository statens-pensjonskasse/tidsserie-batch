package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.TidsperiodeFactory;
import no.spk.felles.tidsperiode.Tidsperiode;

/**
 * @author Snorre E. Brekke - Computas
 */
class PeriodeTypeTestFactory implements TidsperiodeFactory {
    private final Map<Class<?>, List<Tidsperiode<?>>> perioder;

    public PeriodeTypeTestFactory(Tidsperiode<?>... perioder) {
        this.perioder = group(perioder);
    }

    @Override
    public Stream<Tidsperiode<?>> loennsdata() {
        return Stream.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Stream<T> perioderAvType(Class<T> type) {
        return (Stream<T>) getPerioder(type).stream();
    }

    private Map<Class<?>, List<Tidsperiode<?>>> group(Tidsperiode<?>[] perioder) {
        return Stream.of(perioder)
                .collect(groupingBy(p -> p.getClass()));
    }

    private <T> List<Tidsperiode<?>> getPerioder(Class<T> type) {
        return perioder.computeIfAbsent(type, k -> new ArrayList<>());
    }

    public void addPerioder(Tidsperiode<?>... perioder) {
        group(perioder)
                .entrySet()
                .stream()
                .forEach(e -> getPerioder(e.getKey()).addAll(e.getValue()));
    }
}
