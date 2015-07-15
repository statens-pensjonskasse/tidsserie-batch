package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon.avtaleversjon;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;

/**
 * {@link AvtaleversjonOversetter} representerer algoritma
 * for å mappe om og konvertere stillingshistorikk til
 * {@link no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon}
 * <br>
 * Informasjon henta frå avtaleversjonane skal inneholde følgjande verdiar, alle representert som tekst:
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
 * <td>AVTALEVERSJON</td>
 * <td>Typeindikator som identifiserer rada som ein avtaleversjon</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>6-sifret kode</td>
 * <td>Avtalenummeret for avtaleversjonens avtale</td>
 * <td>TORT131.NUM_AVTALE_ID</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>yyyy.MM.dd</td>
 * <td>Frå og med-dato, første dag i perioda avtaleversjonen strekker seg over</td>
 * <td>TORT131.DAT_GYLDIG_FOM</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>Til og med-dato, siste dag i perioda avtaleversjonen strekker seg over</td>
 * <td>TORT131.DAT_GYLDIG_TOM</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>yyyy.MM.dd</td>
 * <td>Avtaleversjonens registreringsdato</td>
 * <td>TORT131.DAT_REGISTRERT</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>1-6-sifra kode / ingenting</td>
 * <td>Premiestatus</td>
 * <td>TORT131.IDE_AVTALE_PREMIEST</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>3-sifra kode</td>
 * <td>Premiekategori</td>
 * <td>TORT131.TYP_PREMIEKATEGORI</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class AvtaleversjonOversetter extends ReflectiveCsvOversetter<AvtaleversjonCsv, Avtaleversjon> implements CsvOversetter<Avtaleversjon> {

    public AvtaleversjonOversetter() {
        super("AVTALEVERSJON", AvtaleversjonCsv.class);
    }

    @Override
    protected Avtaleversjon transformer(final AvtaleversjonCsv csvRad) {
        final AvtaleId avtaleId = csvRad.avtaleid.map(AvtaleId::valueOf).get();
        return avtaleversjon(avtaleId)
                .fraOgMed(csvRad.fraOgMedDato.map(Datoar::dato).get())
                .tilOgMed(csvRad.tilOgMedDato.map(Datoar::dato))
                .premiestatus(csvRad.premiestatus.map(Premiestatus::valueOf).orElse(Premiestatus.UKJENT))
                .premiekategori(
                        csvRad.premiekategori.flatMap(Premiekategori::parse)
                                .orElseThrow(() -> new IllegalStateException(
                                                "koda "
                                                        + csvRad.premiekategori.orElse("")
                                                        + " er ikkje ein gyldig premiekategori"
                                        )
                                )
                )
                .bygg()
                ;
    }
}
