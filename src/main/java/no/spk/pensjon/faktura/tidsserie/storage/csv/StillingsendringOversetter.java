package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForStillingsendring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aksjonskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.DeltidsjustertLoenn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Fastetillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Funksjonstillegg;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Loennstrinn;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingskode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Stillingsprosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Variabletillegg;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;

/**
 * {@link StillingsendringOversetter} representerer algoritma
 * for � mappe om og konvertere stillingshistorikk til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring}
 * <p>
 * Informasjon henta fr� stillingshistorikken skal inneholde f�lgjande verdiar, alle representert som tekst:
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
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>F�dselsdato for medlem</td>
 * <td>TORT016.DAT_KUNDE_FOEDT_NUM</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>5-sifra tall</td>
 * <td>Personnummer for medlem</td>
 * <td>TORT016.IDE_KUNDE_PRSNR</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Long</td>
 * <td>Stillingsforholdnr</td>
 * <td>TORT016.IDE_SEKV_TORT125</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>4-sifra kode</td>
 * <td>Aksjonskoda som n�rmare beskrive kva type stillingsendring det er snakk om</td>
 * <td>TORT016.TYP_AKSJONSKODE</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Long</td>
 * <td>Organisasjonsnummer, ikkje i bruk</td>
 * <td>TORT016.IDE_ARBGIV_NR</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>3-sifra kode</td>
 * <td>Permisjonsavtale for stillingsendringar med aksjonskode 028, 029, 012</td>
 * <td>TORT016.TYP_PERMAVT</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>yyyy.MM.dd</td>
 * <td>Registreringsdato for n�r stillingsendringa vart registrert i PUMA</td>
 * <td>TORT016.DAT_REGISTRERT</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>Double</td>
 * <td>Stillingsprosent for stillinga endringa er tilknytta, er normalt sett ein verdi mellom 0 og 100, men kan for visse historiske �rgangar og yrkesgrupper vere st�rre enn 100</td>
 * <td>TORT016.RTE_DELTID</td>
 * </tr>
 * <tr>
 * <td>9</td>
 * <td>Integer</td>
 * <td>L�nnstrinn, for stillingar som ikkje innrapporterer l�nn blir l�nna innrapportert som l�nnstrinn som kan benyttast for � sl� opp l�nn i 100% stilling.</td>
 * <td>TORT016.NUM_LTR</td>
 * </tr>
 * <tr>
 * <td>10</td>
 * <td>Integer</td>
 * <td>Deltidsjustert, innrapportert l�nn for stillingar som ikkje blir innrapportert med l�nnstrinn</td>
 * <td>TORT016.BEL_LONN</td>
 * </tr>
 * <tr>
 * <td>11</td>
 * <td>Integer</td>
 * <td>Faste l�nnstillegg som blir utbetalt i tillegg til grunnl�nna, skal innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_FTILL</td>
 * </tr>
 * <tr>
 * <td>12</td>
 * <td>Integer</td>
 * <td>Variable l�nnstillegg som blir utbetalt i tillegg til grunnl�nna, skal innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_VTILL</td>
 * </tr>
 * <tr>
 * <td>13</td>
 * <td>Integer</td>
 * <td>Funksjonstillegg som blir utbetalt i tillegg til grunnl�nna, skal ikkje innrapporterast deltidsjustert.</td>
 * <td>TORT016.BEL_FUTILL</td>
 * </tr>
 * <tr>
 * <td>14</td>
 * <td>yyyyMMdd</td>
 * <td>Aksjonsdato, datoen stillingsendringa trer i kraft</td>
 * <td>TORT016.DAT_AKSJON</td>
 * </tr>
 * <tr>
 * <td>15</td>
 * <td>integer</td>
 * <td>Stillingskode</td>
 * <td>TORT016.NUM_STILLINGSKODE</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Tarjei Skorgenes
 */
public class StillingsendringOversetter implements MedlemsdataOversetter<Stillingsendring> {
    /**
     * Type indikator for stillingshistorikk.
     */
    public static final String TYPEINDIKATOR = "0";

    /**
     * Kolonneindeksen stillingsforholdnummer blir henta fr�.
     */
    public static final int INDEX_STILLINGSFORHOLD = 3;

    /**
     * Kolonneindeksen aksjonskode blir henta fr�.
     */
    public static final int INDEX_AKSJONSKODE = 4;

    /**
     * Kolonneindeksen stillingsprosenten blir henta fr�.
     */
    public static final int INDEX_STILLINGSPROSENT = 8;

    /**
     * Kolonneindeksen l�nnstrinn blir henta fr�.
     */
    public static final int INDEX_LOENNSTRINN = 9;

    /**
     * Kolonneindeksen l�nn blir henta fr�.
     */
    public static final int INDEX_LOENN = 10;

    /**
     * Kolonneindeksen faste tillegg blir henta fr�.
     */
    public static final int INDEX_FASTE_TILLEGG = 11;

    /**
     * Kolonneindeksen variable tillegg blir henta fr�.
     */
    public static final int INDEX_VARIABLE_TILLEGG = 12;

    /**
     * Kolonneindeksen funksjonstillegg blir henta fr�.
     */
    public static final int INDEX_FUNKSJONSTILLEGG = 13;

    /**
     * Kolonneindeksen aksjonsdato blir henta fr�.
     */
    public static final int INDEX_AKSJONSDATO = 14;

    /**
     * Kolonneindeksen stillingskode blir henta fr�.
     */
    public static final int INDEX_STILLINGSKODE = 15;

    /**
     * Forventa antall kolonner i ei stillingsendringrad.
     */
    public static final int ANTALL_KOLONNER = INDEX_STILLINGSKODE + 1;

    private final OversetterSupport support = new OversetterSupport();

    /**
     * Oversetter innholdet i <code>rad</code> til ei ny
     * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode}.
     *
     * @param rad avtalekoblinga i tabellformat
     * @return ei ny avtalekoblingsperiode populert med verdiar fr� <code>rad</code>
     */
    @Override
    public Stillingsendring oversett(final List<String> rad) {
        if (rad.size() < ANTALL_KOLONNER) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForStillingsendring(rad)
            );
        }
        final int index = INDEX_AKSJONSDATO;
        return new Stillingsendring()
                .stillingsforhold(read(rad, INDEX_STILLINGSFORHOLD).map(StillingsforholdId::valueOf).get())
                .aksjonskode(read(rad, INDEX_AKSJONSKODE).map(Aksjonskode::valueOf).orElse(Aksjonskode.UKJENT))
                .aksjonsdato(readDato(rad, index).get())
                .stillingsprosent(read(rad, INDEX_STILLINGSPROSENT).map(Prosent::new).map(Stillingsprosent::new).get())
                .stillingskode(read(rad, INDEX_STILLINGSKODE).map(Stillingskode::parse))
                .loennstrinn(readLoennstrinn(rad, INDEX_LOENNSTRINN))
                .loenn(read(rad, INDEX_LOENN).map(Long::valueOf).map(Kroner::new).map(DeltidsjustertLoenn::new))
                .fastetillegg(readFastetillegg(rad, INDEX_FASTE_TILLEGG))
                .variabletillegg(readVariabletillegg(rad, INDEX_VARIABLE_TILLEGG))
                .funksjonstillegg(readFunksjonstillegg(rad, INDEX_FUNKSJONSTILLEGG))
                ;
    }

    /**
     * Inneheld <code>rad</code> stillingshistorikk?
     *
     * @param rad ei rad som inneheld medlemsspesifikk informasjon
     * @return <code>true</code> dersom typeindikatoren matchar typeindikatoren for stillingshistorikk,
     * <code>false</code> ellers
     */
    @Override
    public boolean supports(final List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til l�nnstrinn.
     *
     * @param rad   ei rad som inneheld kolonner med informasjonen som representerer stillingsendringa
     * @param index indeksen som styrer kva kolonne i rada l�nnstrinnverdien blir henta fr�
     * @return endringas l�nnstrinn eller ingenting dersom l�nnstrinn manglar, er tomt eller er lik 0
     */
    Optional<Loennstrinn> readLoennstrinn(final List<String> rad, final int index) {
        return read(rad, index)
                .map(Integer::valueOf)
                .filter(tall -> tall > 0)
                .map(Loennstrinn::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til faste tillegg.
     *
     * @param rad   ei rad som inneheld kolonner med informasjonen som representerer stillingsendringa
     * @param index indeksen som styrer kva kolonne i rada dei faste tillegga blir henta fr�
     * @return endringas faste tillegg eller ingenting dersom faste tillegg manglar, er tomt eller er lik 0
     */
    Optional<Fastetillegg> readFastetillegg(final List<String> rad, final int index) {
        return readValgfrittKronebeloep(rad, index).map(Fastetillegg::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til variable tillegg.
     *
     * @param rad   ei rad som inneheld kolonner med informasjonen som representerer stillingsendringa
     * @param index indeksen som styrer kva kolonne i rada dei variable tillegga blir henta fr�
     * @return endringas variable tillegg eller ingenting dersom variable tillegg manglar, er tomt eller er lik 0
     */
    Optional<Variabletillegg> readVariabletillegg(final List<String> rad, final int index) {
        return readValgfrittKronebeloep(rad, index).map(Variabletillegg::new);
    }

    /**
     * Oversetter innholdet fr� feltet p� den angitte indeksen i rada fr� tekst til funksjonstillegg.
     *
     * @param rad   ei rad som inneheld kolonner med informasjonen som representerer stillingsendringa
     * @param index indeksen som styrer kva kolonne i rada funksjonstillegg blir henta fr�
     * @return endringas variable tillegg eller ingenting dersom funksjonstillegg manglar, er tomt eller er lik 0
     */
    Optional<Funksjonstillegg> readFunksjonstillegg(final List<String> rad, final int index) {
        return readValgfrittKronebeloep(rad, index).map(Funksjonstillegg::new);
    }

    /**
     * @see OversetterSupport#read(List, int)
     */
    Optional<String> read(final List<String> rad, final int index) {
        return support.read(rad, index);
    }

    /**
     * @see OversetterSupport#readDato(List, int)
     */
    Optional<LocalDate> readDato(final List<String> rad, final int index) {
        return support.readDato(rad, index);
    }

    private Optional<Kroner> readValgfrittKronebeloep(final List<String> rad, final int index) {
        return read(rad, index)
                .map(Integer::valueOf)
                .filter(tall -> tall > 0)
                .map(Kroner::new);
    }
}
