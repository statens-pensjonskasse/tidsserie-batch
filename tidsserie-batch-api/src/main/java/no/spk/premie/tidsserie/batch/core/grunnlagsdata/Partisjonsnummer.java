package no.spk.premie.tidsserie.batch.core.grunnlagsdata;

import static java.lang.Math.abs;
import static java.lang.String.format;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Partisjonsnummer {
    private static final int ANTALL_PARTISJONAR = 271;

    private final long partisjonsnummer;

    private Partisjonsnummer(final long partisjonsnummer) {
        assert partisjonsnummer > 0;
        assert partisjonsnummer <= ANTALL_PARTISJONAR;
        this.partisjonsnummer = partisjonsnummer;
    }

    public static Partisjonsnummer partisjonsnummer(final long partisjonsnummer) {
        return new Partisjonsnummer(partisjonsnummer);
    }

    public static Stream<Partisjonsnummer> stream() {
        return
                IntStream
                        .rangeClosed(1, ANTALL_PARTISJONAR)
                        .mapToObj(Partisjonsnummer::new);
    }

    public long index() {
        return partisjonsnummer - 1;
    }

    public long partisjonsnummer() {
        return partisjonsnummer;
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

    public static Partisjonsnummer tilhørendePartisjonForMedlem(final String medlemsId) {
        final byte[] bytes = medlemsId.getBytes(StandardCharsets.UTF_8);
        final long hash = MurmurHash.hash64(
                bytes,
                bytes.length
        );
        final long index = abs(hash) % ANTALL_PARTISJONAR;
        return partisjonsnummer(1 + index);
    }
}
