package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.LoennstrinnBeloep;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForApotekLoennstrinn;

/**
 * {@link ApotekLoennstrinnperiodeOversetter} representerer algoritma
 * for � mappe om og konvertere statlige l�nnstrinn til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode}
 * <p>
 * Informasjon henta fr� l�nnstrinnperioder tilknytta Apotekordninga skal inneholde f�lgjande verdiar, alle representert
 * som tekst:
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
 * <td>yyyy.MM.dd</td>
 * <td>Fr� og med-dato</td>
 * <td>TORT012.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Long</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>TORT012.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>1 - 3-sifra tall</td>
 * <td>L�nnstrinn</td>
 * <td>TORT012.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>1 - 2-sifra tall</td>
 * <td>Stillingskode</td>
 * <td>TORT012.NUM_STILLINGSKODE</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Integer</td>
 * <td>Kronebel�pet l�nnstrinnet tilsvarar innanfor den aktuelle tidsperioda</td>
 * <td>TORT012.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class ApotekLoennstrinnperiodeOversetter {
    /**
     * Typeindikator for rader som inneheld informasjon som kan oversettast til l�nnstrinnperioder for Apotekordninga.
     */
    public static final String TYPEINDIKATOR = "POA_LTR";

    /**
     * Kolonneindeksen fr� og med-dato blir henta fr�.
     */
    public static final int INDEX_FRA_OG_MED_DATO = 1;

    /**
     * Kolonneindeksen til og med-dato blir henta fr�.
     */
    public static final int INDEX_TIL_OG_MED_DATO = 2;

    /**
     * Kolonneindeksen l�nnstrinn blir henta fr�.
     */
    public static final int INDEX_LOENNSTRINN = 3;

    /**
     * Kolonneindeksen stillingskode blir henta fr�.
     */
    public static final int INDEX_STILLINGSKODE = 4;

    /**
     * Kolonneindeksen bel�p blir henta fr�.
     */
    public static final int INDEX_BELOEP = 5;

    /**
     * Antall felt som kvar rad m� inneholde for � bli fors�k konvertert til ei l�nnstrinnperiode for Apotekordninga.
     */
    public static final int ANTALL_FELT = INDEX_BELOEP + 1;

    /**
     * Inneheld <code>rad</code> ei l�nnstrinnperiode for Apotekordninga?
     *
     * @param rad ei rad som skal sjekkast den er ei l�nnstrinnperiode for Apotekordninga
     * @return <code>true</code> dersom typeindikatoren i f�rste kolonne indikerer at det er ei l�nnstrinnperiode for
     * Apotekordninga,  <code>false</code> ellers
     */
    public boolean supports(List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * Oversetter innholdet i <code>rad</code> til ei ny {@link no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode}.
     *
     * @param rad ei l�nnstrinnperiode, sj� klassedefinisjonen for informasjon om forventa kolonner og format
     * @return ei ny l�nnstrinnperiode for Apotekordninga
     */
    public ApotekLoennstrinnperiode oversett(final List<String> rad) {
        if (rad.size() < ANTALL_FELT) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForApotekLoennstrinn(rad)
            );
        }
        return new ApotekLoennstrinnperiode(
                read(rad, INDEX_FRA_OG_MED_DATO).map(Datoar::dato).get(),
                read(rad, INDEX_TIL_OG_MED_DATO).map(Datoar::dato),
                read(rad, INDEX_LOENNSTRINN).map(Loennstrinn::new).get(),
                read(rad, INDEX_STILLINGSKODE).map(Stillingskode::parse).get(),
                read(rad, INDEX_BELOEP).map(Integer::valueOf).map(Kroner::new).map(LoennstrinnBeloep::new).get()
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
        return ofNullable(rad.get(index)).map(String::trim).filter(text -> !text.isEmpty());
    }
}
