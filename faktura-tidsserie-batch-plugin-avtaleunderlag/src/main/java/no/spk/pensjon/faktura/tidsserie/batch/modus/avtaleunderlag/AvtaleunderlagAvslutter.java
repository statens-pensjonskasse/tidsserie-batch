package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Callback som genererer filliste for DVH etter at avtaleunderlaget har blitt generert ferdig.
 *
 * @author Snorre E. Brekke - Computas
 * @see FillisteGenerator
 */
class AvtaleunderlagAvslutter implements TidsserieGenerertCallback {
    private final Path tidserieKatalog;

    AvtaleunderlagAvslutter(final Path tidserieKatalog) {
        this.tidserieKatalog = requireNonNull(tidserieKatalog, "tidserieKatalog kan ikke være null.");
    }


    @Override
    public void tidsserieGenerert(ServiceRegistry serviceRegistry) {
        lagCsvGruppefiler();
    }

    /**
     * @return this for chaning
     * @see FillisteGenerator#genererFilliste(Path)
     */
    private AvtaleunderlagAvslutter lagCsvGruppefiler() {
        new FillisteGenerator().genererFilliste(tidserieKatalog);
        return this;
    }
}
