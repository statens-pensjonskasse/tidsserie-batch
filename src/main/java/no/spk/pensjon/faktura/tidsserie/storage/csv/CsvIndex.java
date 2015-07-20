package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Et felt annotert med CsvIndex indikerer at feltet korresponderer til verdien på angitt index i en liste med strenger.
 * <br>
 * Merk at det kun er tillatt å definere kolonner som valgfrie viss dei ligg etter siste obligatoriske kolonne
 * ettersom det ellers blir tvetydig kva kolonner som er populert eller ikkje.
 * <br>
 * Merk at det og kun er tillatt å utelate verdiar for ei valgfri kolonner etter siste populerte kolonne, igjen
 * fordi det ellers blir tvetydig kva kolonner som inneheld verdiar.
 * <br>
 * Eksempel:
 * <br>
 * Kolonne 4 og 5 er valgfrie, viss kolonne 5 er inkludert i CSV-fila må også kolonne 4 vere inkludert.
 * <br>
 * Kolonne 4 og 5 er valgfrie, viss kolonne 5 ikkje er inkludert i CSV-fila kan også kolonne 4 mangle.
 *
 * @author Snorre E. Brekke - Computas
 * @see StringListToObjectFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.FIELD)
public @interface CsvIndex {
    int value();

    /**
     * Korvidt kolonna er obligatorisk eller ei.
     *
     * @return <code>true</code> dersom kolonna må vere tilstades i CSV-fila, <code>false</code> viss kolonna kan mangle
     */
    boolean obligatorisk() default true;
}
