package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;

/**
 * Representerer dato grunnlagsdata for avtalegrunnlaget ble hentet fra Kasper.
 * @author Snorre E. Brekke - Computas
 */
final class Uttrekksdato {

    private final LocalDate uttrekksdato;

    public Uttrekksdato(LocalDate uttrekksdato) {
        this.uttrekksdato = requireNonNull(uttrekksdato, "dato kan ikke være null");
    }

    /**
     * @return dato grunnlagsdata for avtalegrunnlaget ble hentet fra Kasper.
     */
    public LocalDate uttrekksdato() {
        return uttrekksdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Uttrekksdato)) return false;

        Uttrekksdato that = (Uttrekksdato) o;

        return uttrekksdato != null ? uttrekksdato.equals(that.uttrekksdato) : that.uttrekksdato == null;

    }

    @Override
    public int hashCode() {
        return uttrekksdato != null ? uttrekksdato.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "uttrekk " + uttrekksdato;
    }
}
