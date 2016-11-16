package no.spk.pensjon.faktura.tidsserie.batch.it;

import java.io.File;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;

interface PU_FAK_BA_10 {
    void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus);
}
