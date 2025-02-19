package no.spk.premie.tidsserie.batch.core;


import java.nio.file.Path;

/**
 * {@link Katalog} inneheld ei oversikt over egenskapane som
 * skiller tenester av type {@link Path} frå kvarandre ved oppslag av batchens katalogar
 * via tenesteregisteret.
 * <br>
 * Oversikta er tiltenkt brukt frå modusar eller tenester som har behov for å slå opp
 * ein bestemt type katalog for bruk ved innlesing, lagring eller logging av data frå
 * eller til ein av batchens 3 hovedkatalogar.
 * <br>
 * <h3>Eksempel: Oppslag av batchen sin innkatalog</h3>
 * <code>registry.getServiceReference(Path.class, Katalog.GRUNNLAGSDATA.egenskap())</code>
 *
 * @author Snorre E. Brekke - Computas
 */
public enum Katalog {
    /**
     * Katalog for logfiler.
     */
    LOG("katalog=log"),
    /**
     * Katalog hvor tidsserien lagres.
     */
    UT("katalog=ut"),
    /**
     * Katalog hvor grunnlagsdata som benyttes hentes fra.
     */
    GRUNNLAGSDATA("katalog=grunnlagsdata");

    private final String egenskap;

    Katalog(final String egenskap) {
        this.egenskap = egenskap;
    }

    public String egenskap() {
        return egenskap;
    }
}
