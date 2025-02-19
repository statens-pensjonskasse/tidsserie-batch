package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import java.util.Objects;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

class Nodenummer {
    private final int nodenummer;
    private final int antallNoder;

    Nodenummer(final int nodenummer, final int antallNoder) {
        assert antallNoder >= 1;
        assert nodenummer > 0;
        assert nodenummer <= antallNoder;

        this.nodenummer = nodenummer;
        this.antallNoder = antallNoder;
    }

    static Nodenummer nodenummer(final int nodenummer, final int antallNoder) {
        return new Nodenummer(nodenummer, antallNoder);
    }

    boolean skalHandtere(final Partisjonsnummer partisjon) {
        return partisjon.index() % antallNoder == index();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodenummer, antallNoder);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Nodenummer that = (Nodenummer) o;
        return this.nodenummer == that.nodenummer &&
                this.antallNoder == that.antallNoder;
    }

    @Override
    public String toString() {
        return "node " + (nodenummer) + " av " + antallNoder;
    }

    private int index() {
        return nodenummer - 1;
    }
}
