package no.spk.premie.tidsserie.batch.it;

import java.io.File;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.premie.tidsserie.batch.main.input.Modus;

interface TidsserieBatch {
    void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus);
}
