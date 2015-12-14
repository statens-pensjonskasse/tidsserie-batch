package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Hjelpeklasse for å fjerne exception-støy fra reflection-kode.
 * @author Snorre E. Brekke - Computas
 */
public final class ReflectionUtils {
    private ReflectionUtils(){
        //Kun statiske metoder - skal ikke instansieres
    }

    /**
     * Lager en ny instans av klassen ved å kalle default-konstruktøren (no-args) til klassen.
     * Konstruktøren kan være private.
     * @param classToInstantiate er klassen som skal instansieres
     * @param <T> typen til klassen
     * @return ny instans av T
     * @throws IllegalArgumentException dersom klassen av en eller annen grunn ikke lar seg instansiere
     */
    public static  <T> T newInstance(Class<T> classToInstantiate) {
        try {
            Constructor<T> constructor = classToInstantiate.getDeclaredConstructor();
            boolean access = constructor.isAccessible();
            constructor.setAccessible(true);
            T newCsvInstance = constructor.newInstance();
            constructor.setAccessible(access);
            return newCsvInstance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Setter en verdi i feltet til en instans av en klasse.
     * @param field er et felt som finnes i instansen som skal endres
     * @param value verdien som skal settes i feltet
     * @param instanceToMutate instansen som skal endres
     */
    public static  void setValue(Field field, Object value, Object instanceToMutate) {
        try {
            boolean access = field.isAccessible();
            field.setAccessible(true);
            field.set(instanceToMutate, value);
            field.setAccessible(access);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
