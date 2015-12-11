package no.spk.pensjon.faktura.tidsserie.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hjelpeklasse for å sjekke at alle felt i en klasse har verdi.
 * @author Snorre E. Brekke - Computas
 */
public class FeltVerdiValidering {

    private FeltVerdiValidering(){
        //util-klasse
    }

     /**
     * Sjekker om alle feltene i instansen har en ikke-null verdi.
      * @param instans instansen som skal sjekkes for null-felt.
     * @throws IllegalStateException dersom ett eller flere felt i denne klassen er null.
     */
    public static void validerAlleFelterSatt(Object instans) {
        final String nullVerdier = Arrays.stream(instans.getClass().getDeclaredFields())
                .filter(f -> value(f, instans) == null)
                .map(Field::getName)
                .collect(Collectors.joining(", "));
        if (!nullVerdier.isEmpty()) {
            throw new IllegalStateException("Følgende felt i " + instans.getClass().getSimpleName() + " var ikke angitt: " + nullVerdier);
        }
    }

    private static Object value(Field field, Object instans) {
        try {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            final Object o = field.get(instans);
            field.setAccessible(accessible);
            return o;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
