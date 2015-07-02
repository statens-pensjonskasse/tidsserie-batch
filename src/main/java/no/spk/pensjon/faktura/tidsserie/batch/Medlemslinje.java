package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.batch.Validators.require;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;

/**
 * {@link Medlemslinje} representerer ei rad som inneheld medlemsdata for eit medlem.
 * <br>
 * Kvar medlemslinje startar med ei kolonne som inneheld ein {@link MedlemsId identifikator} som unikt identifiserer
 * medlemmet linja tilhøyrer. Resterande kolonner er reservert til ein av dei medlemsspesifikke datatypene
 * som tidsseriegenereringa benyttar seg av.
 * <br>
 * For dei medlemsspesifikke datatypene er dei neste 3 kolonnene etter medlemsidentifikatoren også felles på tvers
 * av alle datatyper:
 * <ul>
 * <li>Fødselsdato</li>
 * <li>Personnummer</li>
 * <li>Stillingsforhold</li>
 * </ul>
 * <br>
 * Kvar medlemslinje blir derfor validert for å sikre at alle linjer inneheld meir enn 4 felt ettersom linja ellers
 * må vere ugyldig eller verdilaus for vidare prosessering.
 * <br>
 * Medlemslinjene validerer ikkje at datatypen er ein av datatypene som domenemodellen støttar, dette forventast utført
 * i og av domenemodellen på eit seinare tidspunkt i tidsseriegenereringa.
 * <br>
 * For meir informasjon om kva medlemsspesifikke datatyper som er støtta, sjå {@link Medlemsdata} i domenemodellen
 * for tidsserien.
 *
 * @author Tarjei Skorgenes
 */
public class Medlemslinje {
    private final List<String> medlemsdata;

    private final MedlemsId medlem;

    /**
     * Konstruerer ei ny medlemslinje basert på verdiane i <code>values</code>.
     *
     * @param values data for medlemslinja med ein unik medlemsidentifikator i første kolonne, etterfulgt av medlemsdata i dei resterande kolonnene
     * @throws NullPointerException     dersom <code>values</code> er <code>null</code>
     * @throws IllegalArgumentException dersom <code>values</code> ikkje inneheld minst 5 kolonner eller dersom
     *                                  kolonne nr 3 og 4 ikkje inneheld eit syntaktisk gyldig fødsels- og personummer
     */
    public Medlemslinje(final List<String> values) {
        this.medlemsdata = require(
                requireNonNull(values, "verdiar for medlemslinja er påkrevd, men var null")
                        .stream()
                        .skip(1)
                        .collect(toList()),
                value -> values.size() > 4,
                value -> new IllegalArgumentException(
                        "Ei medlemslinje må inneholde minst 5 kolonner med grunnlagsdata, "
                                + "antall kolonner var " + values.size() + "\n"
                                + "Kolonner:\n"
                                + values
                                .stream()
                                .map(cell -> " - " + cell)
                                .collect(joining("\n"))
                )
        );
        this.medlem = MedlemsId.medlemsId(
                values.get(2), // Fødselsdato
                values.get(3)  // Personnummer
        );
    }

    /**
     * Fødselsnummeret for medlemmet linja er tilknytta.
     *
     * @return fødselsnummeret til medlemmet
     */
    public MedlemsId medlem() {
        return medlem;
    }

    /**
     * Tilhøyrer linja det samme medlemmet som medlemmet identifisert av <code>other</code>?
     *
     * @param other eit fødselsnummer som identifiserer eit medlem
     * @return <code>true</code> dersom linja tilhøyrer samme medlem som <code>other</code>,
     * <code>false</code> ellers
     */
    public boolean tilhoeyrer(final MedlemsId other) {
        return medlem.equals(other);
    }


    /**
     * Medlemsdatane som linja inneheld.
     * <br>
     * Den unike medlemsidentifikatoren frå første kolonne på linja, er ikkje med her, kun resterande kolonner på linja.
     *
     * @return alle kolonnene frå linja som inneheld medlemsdata
     */
    public List<String> data() {
        return medlemsdata;
    }

    @Override
    public String toString() {
        return "medlemslinje for medlem " + medlem() + ", medlemsdata = " + data();
    }
}
