package no.spk.felles.tidsserie.batch.core.medlem;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.spk.felles.tidsserie.batch.core.Validators.require;
import static no.spk.felles.tidsserie.batch.core.medlem.MedlemsId.medlemsId;

import java.util.List;

/**
 * {@link Medlemslinje} representerer ei rad som inneheld medlemsdata for eit medlem.
 * <br>
 * Kvar medlemslinje startar med ei kolonne som inneheld ein {@link MedlemsId identifikator} som unikt identifiserer
 * medlemmet linja tilhøyrer. Resterande kolonner er reservert til ein av dei medlemsspesifikke datatypene
 * som tidsseriegenereringa benyttar seg av.
 * <br>
 * Ingen vidare validering blir gjort på medlemslinjenes innhold, det er heilt opp til dei forskjellige modusane
 * å definere kva krav dei har til medlemsdatane dei skal behandle.
 * <br>
 * Medlemslinjene validerer ikkje at datatypen er ein av datatypene som domenemodellen støttar, dette forventast utført
 * i og av domenemodellen på eit seinare tidspunkt i tidsseriegenereringa.
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
     * @throws NullPointerException dersom <code>values</code> er <code>null</code>
     * @throws IllegalArgumentException dersom <code>values</code> ikkje inneheld minst 5 kolonner eller dersom
     * kolonne nr 3 og 4 ikkje inneheld eit syntaktisk gyldig fødsels- og personummer
     */
    public Medlemslinje(final List<String> values) {
        this.medlemsdata = require(
                requireNonNull(values, "verdiar for medlemslinja er påkrevd, men var null")
                        .stream()
                        .skip(1)
                        .collect(toList()),
                value -> values.size() >= 1,
                value -> new IllegalArgumentException(
                        String.format(
                                "Ei medlemslinje må inneholde minst 1 kolonne med medlemsdata, antall kolonner var %s\nKolonner:\n%s",
                                values.size(),
                                kolonneverdierForFeilmelding(values))

                )
        );

        this.medlem = medlemsId(
                require(
                        requireNonNull(
                                values.get(0),
                                () -> feilmeldingVedUgyldigMedlemsId(values)
                        )
                                .trim(),
                        this::ikkjeTom,
                        value -> new IllegalArgumentException(
                                feilmeldingVedUgyldigMedlemsId(values)
                        )
                )
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

    private String feilmeldingVedUgyldigMedlemsId(List<String> values) {
        return String.format(
                "Kolonne for medlemsid kan ikkje inneholde verdien '%s' \nKolonner:\n%s",
                formaterForFeil(values.get(0)),
                kolonneverdierForFeilmelding(values)
        );
    }

    private String kolonneverdierForFeilmelding(List<String> values) {
        return values
                .stream()
                .map(cell -> " - " + formaterForFeil(cell))
                .collect(joining("\n"));
    }

    private boolean ikkjeTom(String value) {
        return value.length() > 0;
    }

    private String formaterForFeil(String cell) {
        if (cell == null) {
            return "<null>";
        }

        if (cell.trim().isEmpty()) {
            return "<tom streng>";
        }
        return cell;
    }
}
