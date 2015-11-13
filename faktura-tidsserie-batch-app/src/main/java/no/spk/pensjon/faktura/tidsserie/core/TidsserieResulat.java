package no.spk.pensjon.faktura.tidsserie.core;

import static no.spk.pensjon.faktura.tidsserie.core.FeltVerdiValidering.validerAlleFelterSatt;

import java.nio.file.Path;

/**
 * Klasse som holder på relevant tilstand for gjennomført tidsseriekjøring.
 * @author Snorre E. Brekke - Computas
 */
public class TidsserieResulat {
    private final Path tidsserieKatalog;

    private TidsserieResulat(TidsserieResultBuilder builder) {
        this.tidsserieKatalog = builder.tidsserieKatalog;
    }

    public static TidsserieResultBuilder tidsserieResulat(Path tidsserieKatalog){
        return new TidsserieResultBuilder().tidsserieKatalog(tidsserieKatalog);
    }

    /**
     * Katalogen hvor tidsserie.csv-filene lagres.
     * @return sti til tidsserieKatalog
     */
    public Path tidsserieKatalog() {
        return tidsserieKatalog;
    }

    public static class TidsserieResultBuilder{

        private Path tidsserieKatalog;

        public TidsserieResultBuilder tidsserieKatalog(Path tidsserieKatalog) {
            this.tidsserieKatalog = tidsserieKatalog;
            return this;
        }

        public TidsserieResulat bygg() {
            validerAlleFelterSatt(this);
            return new TidsserieResulat(this);
        }
    }
}
