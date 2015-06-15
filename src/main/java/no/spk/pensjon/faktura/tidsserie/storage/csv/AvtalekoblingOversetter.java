package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForAvtalekobling;

/**
 * {@link AvtalekoblingOversetter} representerer algoritma
 * for � mappe om og konvertere avtalekoblingar til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode}.
 * <p>
 * Ei avtalekobling skal inneholde f�lgjande verdiar, alle representert som tekst:
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
 * <td>Typeindikator som identifiserer rada som ei avtalekobling</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>F�dselsdato for medlem</td>
 * <td>TORT126.DAT_KUNDE_FOEDT_NUM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>5-sifra tall</td>
 * <td>Personnummer for medlem</td>
 * <td>TORT126.IDE_KUNDE_PRSNR</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>Stillingsforholdnr</td>
 * <td>TORT126.IDE_SEKV_TORT125</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>yyyy.MM.dd</td>
 * <td>Startdato, f�rste dag i perioda stillingsforholdet er tilknytta avtalen</td>
 * <td>TORT126.DAT_START</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>Sluttdato, siste dag i perioda stillingsforholdet er tilknytta avtalen</td>
 * <td>TORT126.DAT_SLUTT</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>6-sifra tall</td>
 * <td>Avtalenummer, avtalen stillingsforholdet er tilknytta i den aktuelle perioda</td>
 * <td>TORT126.NUM_AVTALE_ID</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class AvtalekoblingOversetter implements MedlemsdataOversetter<Avtalekoblingsperiode> {
    /**
     * Type indikator for avtalekoblingar.
     */
    public static final String TYPEINDIKATOR = "1";

    /**
     * Kolonneindeksen stillingsforholdnummer blir henta fr�.
     */
    public static final int INDEX_STILLINGSFORHOLD = 3;

    /**
     * Kolonneindeksen startdato blir henta fr�.
     */
    public static final int INDEX_STARTDATO = 4;

    /**
     * Kolonneindeksen sluttdato blir henta fr�.
     */
    public static final int INDEX_SLUTTDATO = 5;

    /**
     * Kolonneindeksen avtalenummer blir henta fr�.
     */
    public static final int INDEX_AVTALE = 6;

    /**
     * Kolonneindeksen ordning blir henta fr�.
     */
    public static final int INDEX_ORDNING = 7;

    /**
     * Forventa antall kolonner i ei avtalekoblingsrad.
     */
    public static final int ANTALL_KOLONNER = INDEX_ORDNING + 1;

    private final OversetterSupport support = new OversetterSupport();

    /**
     * Oversetter innholdet i <code>rad</code> til ei ny
     * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode}.
     *
     * @param rad avtalekoblinga i tabellformat
     * @return ei ny avtalekoblingsperiode populert med verdiar fr� <code>rad</code>
     */
    @Override
    public Avtalekoblingsperiode oversett(final List<String> rad) {
        if (rad.size() < ANTALL_KOLONNER) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForAvtalekobling(rad)
            );
        }
        return new Avtalekoblingsperiode(
                readDato(rad, INDEX_STARTDATO).get(),
                readDato(rad, INDEX_SLUTTDATO),
                read(rad, INDEX_STILLINGSFORHOLD).map(StillingsforholdId::valueOf).get(),
                read(rad, INDEX_AVTALE).map(AvtaleId::valueOf).get(),
                read(rad, INDEX_ORDNING).map(Ordning::valueOf).get()
        );
    }

    /**
     * Inneheld <code>rad</code> informasjon om ei avtalekobling?
     *
     * @param rad ei rad som inneheld medlemsspesifikk informasjon
     * @return <code>true</code> dersom typeindikatoren matchar typeindikatoren for avtalekoblingar,
     * <code>false</code> ellers
     */
    @Override
    public boolean supports(List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * @see OversetterSupport#readDato(List, int)
     */
    Optional<LocalDate> readDato(final List<String> rad, final int index) {
        return support.readDato(rad, index);
    }

    /**
     * @see OversetterSupport#read(List, int)
     */
    Optional<String> read(final List<String> rad, final int index) {
        return support.read(rad, index);
    }
}
