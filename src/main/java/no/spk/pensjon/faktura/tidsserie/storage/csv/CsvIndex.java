package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Et felt annotert med CsvIndex indikerer at feltet korresponderer til verdien på angitt index i en liste med strenger.
 * @author Snorre E. Brekke - Computas
 * @see StringListToObjectFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.FIELD)
public @interface CsvIndex {
    int value();
}
