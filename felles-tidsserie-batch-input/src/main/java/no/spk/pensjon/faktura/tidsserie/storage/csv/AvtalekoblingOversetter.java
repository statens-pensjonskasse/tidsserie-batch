package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.time.LocalDate;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;

/**
 * {@link AvtalekoblingOversetter} representerer algoritma
 * for å mappe om og konvertere avtalekoblingar til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode}.
 * <p>
 * Ei avtalekobling skal inneholde følgjande verdiar, alle representert som tekst:
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
 * <td>1</td>
 * <td>Typeindikator som identifiserer rada som ei avtalekobling</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>yyyy.MM.dd</td>
 * <td>Fødselsdato for medlem</td>
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
 * <td>Startdato, første dag i perioda stillingsforholdet er tilknytta avtalen</td>
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
public class AvtalekoblingOversetter extends ReflectiveCsvOversetter<AvtalekoblingCsv, Avtalekoblingsperiode> implements MedlemsdataOversetter<Avtalekoblingsperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public AvtalekoblingOversetter() {
        super("1", AvtalekoblingCsv.class);
    }

    @Override
    protected Avtalekoblingsperiode transformer(AvtalekoblingCsv csvRad) {
        return new Avtalekoblingsperiode(
                tilDato(csvRad.startDato).get(),
                tilDato(csvRad.sluttDato),
                csvRad.stillingsforhold.map(StillingsforholdId::valueOf).get(),
                csvRad.avtale.map(AvtaleId::valueOf).get(),
                csvRad.ordning.map(Ordning::valueOf).get()
        );
    }


    /**
     * @see OversetterSupport#tilDato(Optional)
     */
    Optional<LocalDate> tilDato(Optional<String> string) {
        return support.tilDato(string);
    }
}
