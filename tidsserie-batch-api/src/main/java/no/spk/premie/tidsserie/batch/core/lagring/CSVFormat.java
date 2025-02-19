package no.spk.premie.tidsserie.batch.core.lagring;

import java.util.Set;
import java.util.stream.Stream;

import no.spk.felles.tidsperiode.underlag.BeregningsRegel;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;

/**
 * {@link CSVFormat} tilrettelegger for oppbygging av tidsseriemålingar på CSV-format.
 * <br>
 * Formatet er spesifikt ansvarlig for kva kolonner CSV-formatet inneheld og uthenting/beregning av verdiar
 * for kvar kolonne for alle underlagsperioder som inngår i observasjonsunderlaga tidsserien er basert på.
 *
 * @author Tarjei Skorgenes
 */
public interface CSVFormat {
    /**
     * Serialiserer <code>periode</code> til ein straum av kolonneverdiar.
     * <br>
     * Kvar kolonne angitt av {@link #kolonnenavn()} skal få ein verdi uthenta frå <code>observasjonsunderlag</code>,
     * <code>perioder</code> enten direkte via desse sine annotasjonar, eller via {@link BeregningsRegel reglane} som
     * periodene er satt opp med.
     *
     * @param observasjonsunderlag observasjonsunderlaget perioda tilhøyrer
     * @param periode              underlagsperioda som inneheld annotasjonane og beregningsreglane som kolonneverdiane skal hentast frå eller utledast via
     * @return ein straum av kolonneverdiar, ein pr kolonne angitt av {@link #kolonnenavn()}
     * @see #kolonnenavn()
     */
    Stream<Object> serialiser(Underlag observasjonsunderlag, Underlagsperiode periode);

    /**
     * Serialiserer <code>periode</code> til ein straum av kolonneverdiar.
     * <br>
     * Kvar kolonne angitt av {@link #kolonnenavn()} som også finnes i <code>kolonnenavnfilter</code> skal få ein verdi uthenta
     * frå <code>observasjonsunderlag</code>,
     * <code>perioder</code> enten direkte via desse sine annotasjonar, eller via {@link BeregningsRegel reglane} som
     * periodene er satt opp med.
     *
     * @param observasjonsunderlag observasjonsunderlaget perioda tilhøyrer
     * @param periode              underlagsperioda som inneheld annotasjonane og beregningsreglane som kolonneverdiane skal hentast frå eller utledast via
     * @param kolonnenavnfilter    berre kolonner angitt i kolonnenavnfilter blir serialisert - rekkefølgen er avhengig av rekkefølgen på {@link #kolonnenavn()}
     * @return ein straum av kolonneverdiar, ein pr kolonne angitt av {@link #kolonnenavn()}
     * @see #kolonnenavn()
     * @see #serialiser(Underlag, Underlagsperiode)
     */
    Stream<Object> serialiser(Underlag observasjonsunderlag, Underlagsperiode periode, Set<String> kolonnenavnfilter);

    /**
     * Returnerer ein straum som inneheld navnet til kvar kolonne som inngår i CSV-formatet.
     * <br>
     * Metoda forventast brukt til generering av header-linje i filer der dei serialiserte målingane
     * {@link #serialiser(Underlag, Underlagsperiode)} genererer, blir lagra.
     *
     * @return ein straum med kolonnenavn for CSV-formatet, ei kolonne pr verdi som
     * {@link #serialiser(Underlag, Underlagsperiode)} returnerer, angitt i samme rekkefølge
     */
    Stream<String> kolonnenavn();
}
