package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForStatligLoennstrinn;

/**
 * {@link StatligLoennstrinnperiodeOversetter} representerer algoritma
 * for � mappe om og konvertere statlige l�nnstrinn til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode}
 * <p>
 * Informasjon henta fr� statlige l�nnstrinnperioder skal inneholde f�lgjande verdiar, alle representert som tekst:
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Blir typisk henta fr�</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>{@linkplain #TYPEINDIKATOR}</td>
 * <td>Typeindikator som identifiserer rada som ei statlig l�nnstrinnperiode</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1 - 3-sifra tall</td>
 * <td>L�nnstrinn</td>
 * <td>TORT011.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>yyyy.MM.dd</td>
 * <td>Fr� og med-dato</td>
 * <td>TORT011.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT011.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>Kronebel�pet l�nnstrinnet tilsvarar innanfor den aktuelle tidsperioda</td>
 * <td>TORT011.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class StatligLoennstrinnperiodeOversetter {
    /**
     * Typeindikator for rader som inneheld informasjon som kan oversettast til statlige l�nnstrinnperioder.
     */
    public static final String TYPEINDIKATOR = "SPK_LTR";

    /**
     * Kolonneindeksen fr� og med-dato blir henta fr�.
     */
    public static final int INDEX_FRA_OG_MED_DATO = 2;

    /**
     * Kolonneindeksen til og med-dato blir henta fr�.
     */
    public static final int INDEX_TIL_OG_MED_DATO = 3;

    /**
     * Kolonneindeksen l�nnstrinn blir henta fr�.
     */
    public static final int INDEX_LOENNSTRINN = 1;

    /**
     * Kolonneindeksen bel�p blir henta fr�.
     */
    public static final int INDEX_BELOEP = 4;

    /**
     * Inneheld <code>rad</code> ei statlig l�nnstrinnperiode?
     *
     * @param rad ei rad som skal sjekkast den er ei statlig l�nnstrinnperiode
     * @return <code>true</code> dersom typeindikatoren i f�rste kolonne indikerer at det er ei statlig l�nnstrinnperiode
     * <code>false</code> ellers
     */
    public boolean supports(final List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * Oversetter innholdet i <code>rad</code> til ei ny {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode}.
     *
     * @param rad ei statlig l�nnstrinnperiode, sj� klassedefinisjonen for informasjon om forventa kolonner og format
     * @return ei ny statlig l�nnstrinnperiode
     */
    public StatligLoennstrinnperiode oversett(final List<String> rad) {
        if (rad.size() != INDEX_BELOEP + 1) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForStatligLoennstrinn(rad)
            );
        }
        return new StatligLoennstrinnperiode(
                read(rad, INDEX_FRA_OG_MED_DATO).map(Datoar::dato).get(),
                read(rad, INDEX_TIL_OG_MED_DATO).map(Datoar::dato),
                read(rad, INDEX_LOENNSTRINN).map(Loennstrinn::new).get(),
                read(rad, INDEX_BELOEP).map(Integer::valueOf).map(Kroner::new).get()
        );
    }

    /**
     * Hentar ut den tekstlige verdien fr� den angitte indeksen. Dersom verdien er <code>null</code> eller
     * inneheld kun whitespace, eventuelt er heilt tom, blir ein {@link java.util.Optional#empty() tom} verdi returnert.
     *
     * @param rad   rada som verdien skal hentast fr�
     * @param index indexen som peikar til feltet som verdien skal hentast fr�
     * @return den tekstlige verdien av feltet p� den angitte indeksen i rada, eller ingenting dersom feltets verdi
     * er <code>null</code>, eller dersom det kun inneheld whitespace eller verdien er ein tom tekst-streng
     */
    private Optional<String> read(final List<String> rad, final int index) {
        return ofNullable(rad.get(index)).map(String::trim).filter(t -> !t.isEmpty());
    }
}
