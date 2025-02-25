package no.spk.tidsserie.batch.it;

import java.io.File;

import no.spk.tidsserie.tidsperiode.underlag.Observasjonsperiode;
import no.spk.tidsserie.batch.main.input.Modus;

interface TidsserieBatch {
    void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus);
}
