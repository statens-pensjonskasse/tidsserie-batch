package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;

import java.util.stream.IntStream;
import java.util.stream.Stream;

class Partisjonsnummer {
    private static final int ANTALL_PARTISJONAR = 271;

    private final long partisjonsnummer;

    private Partisjonsnummer(final long partisjonsnummer) {
        assert partisjonsnummer > 0;
        assert partisjonsnummer <= ANTALL_PARTISJONAR;
        this.partisjonsnummer = partisjonsnummer;
    }

    static Partisjonsnummer partisjonsnummer(final long partisjonsnummer) {
        return new Partisjonsnummer(partisjonsnummer);
    }

    static Stream<Partisjonsnummer> stream() {
        return
                IntStream
                        .rangeClosed(1, ANTALL_PARTISJONAR)
                        .mapToObj(Partisjonsnummer::new);
    }

    long index() {
        return partisjonsnummer - 1;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(partisjonsnummer);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Partisjonsnummer that = (Partisjonsnummer) o;
        return this.partisjonsnummer == that.partisjonsnummer;
    }

    @Override
    public String toString() {
        return format("partisjon %d av %d", partisjonsnummer, ANTALL_PARTISJONAR);
    }
}
