package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Optional.ofNullable;
import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForOmregningsperiode;

import java.util.List;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode;

/**
 * {@link OmregningsperiodeOversetter} representerer algoritma
 * for å mappe om og konvertere omregningsperioder til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode}
 * <p>
 * Informasjon henta frå statlige lønnstrinnperioder skal inneholde følgjande verdiar, alle representert som tekst:
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Blir typisk henta frå</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>{@linkplain #TYPEINDIKATOR}</td>
 * <td>Typeindikator som identifiserer rada som ei omregningsperiode</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato</td>
 * <td>TORT024.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT024.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Integer</td>
 * <td>Grunnbeløpet</td>
 * <td>TORT023.BEL_G</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */

public class OmregningsperiodeOversetter implements CsvOversetter<Omregningsperiode> {
    /**
     * Typeindikator for rader som inneheld informasjon som kan oversettast til omregningsperioder.
     */
    public static final String TYPEINDIKATOR = "OMREGNING";

    /**
     * Kolonneindeksen frå og med-dato blir henta frå.
     */
    public static final int INDEX_FRA_OG_MED_DATO = 1;

    /**
     * Kolonneindeksen til og med-dato blir henta frå.
     */
    public static final int INDEX_TIL_OG_MED_DATO = 2;

    /**
     * Kolonneindeksen grunnbeløpet blir henta frå.
     */
    public static final int INDEX_GRUNNBELOEP = 3;

    private static final int ANTALL_KOLONNER = INDEX_GRUNNBELOEP + 1;

    /**
     * Inneheld <code>rad</code> ei omregningsperiode?
     *
     * @param rad ei rad som skal sjekkast den er ei omregningsperiode
     * @return <code>true</code> dersom typeindikatoren i første kolonne indikerer at det er ei omregningsperiode
     * <code>false</code> ellers
     */
    @Override
    public boolean supports(final List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * Oversetter innholdet i <code>rad</code> til ei ny {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode}.
     *
     * @param rad ei omregningsperiode, sjå klassedefinisjonen for informasjon om forventa kolonner og format
     * @return ei ny omregningsperiode
     */
    @Override
    public Omregningsperiode oversett(final List<String> rad) {
        if (rad.size() < ANTALL_KOLONNER) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForOmregningsperiode(rad)
            );
        }
        return new Omregningsperiode(
                read(rad, INDEX_FRA_OG_MED_DATO).map(Datoar::dato).get(),
                read(rad, INDEX_TIL_OG_MED_DATO).map(Datoar::dato),
                read(rad, INDEX_GRUNNBELOEP).map(Integer::valueOf).map(Kroner::new).get()
        );
    }

    /**
     * Hentar ut den tekstlige verdien frå den angitte indeksen. Dersom verdien er <code>null</code> eller
     * inneheld kun whitespace, eventuelt er heilt tom, blir ein {@link java.util.Optional#empty() tom} verdi returnert.
     *
     * @param rad   rada som verdien skal hentast frå
     * @param index indexen som peikar til feltet som verdien skal hentast frå
     * @return den tekstlige verdien av feltet på den angitte indeksen i rada, eller ingenting dersom feltets verdi
     * er <code>null</code>, eller dersom det kun inneheld whitespace eller verdien er ein tom tekst-streng
     */
    private Optional<String> read(final List<String> rad, final int index) {
        return ofNullable(rad.get(index)).map(String::trim).filter(t -> !t.isEmpty());
    }
}
