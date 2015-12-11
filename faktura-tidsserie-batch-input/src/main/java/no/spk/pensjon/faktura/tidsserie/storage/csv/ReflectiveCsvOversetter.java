package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.List;

/**
 * Hjelpeklasse for å fjerne nødvendigheten av å implementere {@link #supports(List)} support} for klasser som ønsker å benytte {@link StringListToObjectFactory}.
 * @author Snorre E. Brekke - Computas
 * @see CsvOversetter
 * @param <C> klassen som brukes for overgangen fra en liste med strenger til T
 * @param <T> den endelige resulattypen når en strengliste oversettes
 */
public abstract class ReflectiveCsvOversetter<C, T>{
    private final StringListToObjectFactory<C> factory;

    public ReflectiveCsvOversetter(String type, Class<C> stringToCsvClass) {
        factory = new StringListToObjectFactory<>(type, stringToCsvClass);
    }

    /**
     * Default implementasjon som kan brukes for klasser som arver fra {@link ReflectiveCsvOversetter} og samtidig implementerer {@link CsvOversetter}
     * @param rad rad som skal oversettes
     * @return tru dersom raden er støttet av oversetteren
     */
    public boolean supports(List<String> rad) {
        return factory.supports(rad);
    }

    /**
     * Oversetter en rad til T
     * @param rad som skal oversettes
     * @return raden oversatt til T via C
     */
    public final T oversett(List<String> rad) {
        C csvRad = factory.transform(rad);
        return transformer(csvRad);
    }

    /**
     * @param csvRad instans som holder på relevant data for å kunne lage T
     * @return T
     */
    protected abstract T transformer(C csvRad);
}
