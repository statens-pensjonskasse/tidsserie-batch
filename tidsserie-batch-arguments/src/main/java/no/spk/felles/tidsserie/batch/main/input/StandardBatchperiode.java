package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.Math.max;

import java.time.LocalDate;

/**
 * Lager standardverdier for tidsserie-batchens fra- og til-år.
 * Kun tiltenkt intern bruk.
 */
class StandardBatchperiode {
    private final int fraAar;
    private final int tilAar;

    StandardBatchperiode(final LocalDate currentDate) {
        this.tilAar = currentDate.getYear();
        if (this.tilAar < 2015) {
            throw new IllegalArgumentException("Denne klassen er tiltenkt bruk med LocalDate.now(), og er ikke testet for datoer før 2015.");
        }
        this.fraAar = max(2007, this.tilAar - 9);
    }

    public int fraAar() {
        return fraAar;
    }

    public int tilAar() {
        return tilAar;
    }
}
