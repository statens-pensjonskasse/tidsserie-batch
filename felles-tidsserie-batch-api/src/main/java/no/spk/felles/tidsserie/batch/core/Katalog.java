package no.spk.felles.tidsserie.batch.core;


import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link java.nio.file.Path}'er som brukes av tidsserie-batchen registrers
 * i {@link ServiceRegistry} med egenskap gitt av {@link #egenskap()}.
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

    public String egenskap(){
        return egenskap;
    }
}
