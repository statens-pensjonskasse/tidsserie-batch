package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForMedregningsperiode;

import java.util.List;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Medregningskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;

/**
 * {@link MedregningsOversetter} representerer algoritma
 * for å mappe om og konvertere medregningar til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode}
 * <p>
 * Informasjon henta frå medregningane skal inneholde følgjande verdiar, alle representert som tekst:
 * <table summary="">
 * <thead>
 * <tr>
 * <td>Index</td>
 * <td>Verdi / Format</td>
 * <td>Beskrivelse</td>
 * <td>Kilde</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>{@linkplain #TYPEINDIKATOR}</td>
 * <td>Typeindikator som identifiserer rada som ei stillingsendring</td>
 * <td>Hardkoda, tallet 2</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>Fødselsdato for medlem</td>
 * <td>TORT014.DAT_KUNDE_FOEDT_NUM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>5-sifra tall</td>
 * <td>Personnummer for medlem</td>
 * <td>TORT014.IDE_KUNDE_PRSNR</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>Stillingsforholdnr</td>
 * <td>TORT014.IDE_SEKV_TORT125</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato</td>
 * <td>TORT014.DAT_FRA</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>yyyy.MM.dd</td>
 * <td>Til og med-dato</td>
 * <td>TORT014.DAT_TIL</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>2-sifra kode</td>
 * <td>Medregninskoda som nærmare beskriv kva type medregning det er snakk om</td>
 * <td>TORT014.TYP_KODE</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>Lønn</td>
 * <td>TORT014.BEL_LONN</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class MedregningsOversetter implements MedlemsdataOversetter<Medregningsperiode> {
    /**
     * Type indikator for stillingshistorikk.
     */
    public static final String TYPEINDIKATOR = "2";

    /**
     * Kolonneindeksen stillingsforholdnummer blir henta frå.
     */
    public static final int INDEX_STILLINGSFORHOLD = 3;

    /**
     * Kolonneindeksen fra og med-dato blir henta frå.
     */
    public static final int INDEX_FRA_OG_MED_DATO = 4;

    /**
     * Kolonneindeksen til og med-dato blir henta frå.
     */
    public static final int INDEX_TIL_OG_MED_DATO = 5;

    /**
     * Kolonneindeksen medregningskoda blir henta frå.
     */
    public static final int INDEX_KODE = 6;

    /**
     * Kolonneindeksen lønna blir henta frå.
     */
    public static final int INDEX_LOENN = 7;
    /**
     * Forventa antall kolonner i ei medregningsrad.
     */
    public static final int ANTALL_KOLONNER = INDEX_LOENN + 1;

    private final OversetterSupport support = new OversetterSupport();

    /**
     * Konverterer rada om til ei ny medregningsperiode.
     *
     * @param rad ei rad som inneheld informasjon om ei medregningsperiode
     * @return ei ny medregningsperiode populert med verdiar frå <code>rad</code>
     */
    @Override
    public Medregningsperiode oversett(final List<String> rad) {
        if (rad.size() < ANTALL_KOLONNER) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForMedregningsperiode(rad)
            );
        }
        return new Medregningsperiode(
                read(rad, INDEX_FRA_OG_MED_DATO).map(Datoar::dato).get(),
                read(rad, INDEX_TIL_OG_MED_DATO).map(Datoar::dato),
                read(rad, INDEX_LOENN).map(Integer::valueOf).map(Kroner::new).map(Medregning::new).get(),
                read(rad, INDEX_KODE).map(Integer::valueOf).map(Medregningskode::valueOf).get(),
                read(rad, INDEX_STILLINGSFORHOLD).map(Long::valueOf).map(StillingsforholdId::valueOf).get()
        );
    }

    /**
     * Inneheld rada informasjon om ei medregningsperiode?
     * <p>
     * Kva rader som inneheld medregningsperioder blir bestemt av verdien i kolonne 1, viss den er lik
     * {@link #TYPEINDIKATOR} blir rada forventa å inneholde informasjon om ei medregningsperiode.
     *
     * @param rad ei rad som inneheld medlemsspesifikk informasjon
     * @return <code>true</code> dersom verdien av kolonne 1 er lik {@link #TYPEINDIKATOR}, <code>false</code> ellers
     */
    @Override
    public boolean supports(final List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * @see OversetterSupport#read(List, int)
     */
    private Optional<String> read(final List<String> rad, final int index) {
        return support.read(rad, index);
    }

}
