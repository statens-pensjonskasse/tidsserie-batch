package no.spk.felles.tidsserie.batch.core;

import java.util.List;

import no.spk.felles.tidsperiode.Tidsperiode;

/**
 * Tenester som implementerer {@link CsvOversetter} støttar konvertering av rader
 * frå CSV-format til {@link Tidsperiode tidsperioder} av ein bestemt type.
 * <br>
 * Oversetteren er tiltenkt brukt ved konvertering frå CSV-format ved uthenting av
 * {@link CSVInput#referansedata()}.
 *
 * @param <T> tidsperiodetypen som oversetteren støttar konvertering av
 * @see CSVInput
 */
public interface CsvOversetter<T extends Tidsperiode<?>> {
    /**
     * Støttar oversetteren deserialisering av den typen data
     * som den angitte rada inneheld?
     * <br>
     * Dersom {@code true} blir returnert, forventast det at
     * {@link #oversett(List)} er i stand til å konvertere radas innhold
     * til ei tidsperiode
     *
     * @param rad ei rad som kan inneholde ei eller anna form for data
     * tilhøyrande ei serialisert tidsperiode av ein bestemt type
     * @return <code>true</code> dersom oversetteren støttar oversetting
     * av den aktuelle radas innhold til ei tidsperiode av ein bestemt type,
     * <code>false</code> ellers
     */
    boolean supports(List<String> rad);

    /**
     * Bygger ei ny tidsperiode og populerer den med innhold frå den angitte rada.
     * <br>
     * Oversetteren kan forvente at den kun vil motta rader som den tidligare har
     * indikert at den støttar via {@link #supports(List)}.
     *
     * @param rad ei rad som inneheld data for tidsperiodetypen som oversetteren
     * har sagt at den støttar
     * @return ein ny instans av tidsperiodetypen som oversetteren støttar, populert
     * med tilhøyrande verdiar henta ut frå <code>rad</code>
     */
    T oversett(List<String> rad);
}
