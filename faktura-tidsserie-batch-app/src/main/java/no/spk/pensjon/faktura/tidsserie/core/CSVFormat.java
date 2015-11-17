package no.spk.pensjon.faktura.tidsserie.core;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.underlag.BeregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link CSVFormat} tilrettelegger for oppbygging av tidsseriem�lingar p� CSV-format.
 * <br>
 * Formatet er spesifikt ansvarlig for kva kolonner CSV-formatet inneheld og uthenting/beregning av verdiar
 * for kvar kolonne for alle underlagsperioder som inng�r i observasjonsunderlaga tidsserien er basert p�.
 *
 * @author Tarjei Skorgenes
 */
public interface CSVFormat {
    /**
     * Serialiserer <code>periode</code> til ein straum av kolonneverdiar.
     * <br>
     * Kvar kolonne angitt av {@link #kolonnenavn()} skal f� ein verdi uthenta fr� <code>observasjonsunderlag</code>,
     * <code>perioder</code> enten direkte via desse sine annotasjonar, eller via {@link BeregningsRegel reglane} som
     * periodene er satt opp med.
     *
     * @param observasjonsunderlag observasjonsunderlaget perioda tilh�yrer
     * @param periode              underlagsperioda som inneheld annotasjonane og beregningsreglane som kolonneverdiane skal hentast fr� eller utledast via
     * @return ein straum av kolonneverdiar, ein pr kolonne angitt av {@link #kolonnenavn()}
     * @see #kolonnenavn()
     */
    Stream<Object> serialiser(Underlag observasjonsunderlag, Underlagsperiode periode);

    /**
     * Returnerer ein straum som inneheld navnet til kvar kolonne som inng�r i CSV-formatet.
     * <br>
     * Metoda forventast brukt til generering av header-linje i filer der dei serialiserte m�lingane
     * {@link #serialiser(Underlag, Underlagsperiode)} genererer, blir lagra.
     *
     * @return ein straum med kolonnenavn for CSV-formatet, ei kolonne pr verdi som
     * {@link #serialiser(Underlag, Underlagsperiode)} returnerer, angitt i samme rekkef�lge
     */
    Stream<String> kolonnenavn();
}
