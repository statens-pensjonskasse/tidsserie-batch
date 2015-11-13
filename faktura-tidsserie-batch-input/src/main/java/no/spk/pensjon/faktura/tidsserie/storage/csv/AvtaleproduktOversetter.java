package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Risikoklasse;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

/**
 * {@link AvtaleproduktOversetter} representerer algoritma
 * for å mappe om og konvertere stillingshistorikk til
 * {@link Avtaleprodukt}
 * <br>
 * Informasjon henta frå avtaleprodukt skal inneholde følgjande verdiar, alle representert som tekst:
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
 * <td>AVTALEPRODUKT</td>
 * <td>Typeindikator som identifiserer rada som eit avtaleprodukt</td>
 * <td>Hardkoda</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>6-sifret kode</td>
 * <td>Avtalenummeret for avtaleproduktets avtale</td>
 * <td>TORT023.NUM_AVTALE_ID</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>3-sifra kode</td>
 * <td>Kode som identifiserer kva produkt avtaleproduktet er tilknytta</td>
 * <td>TORT023.IDE_ARBGIV_PROD</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>yyyy.MM.dd</td>
 * <td>Fra og med-dato, første dag i perioda avtaleproduktet strekker seg over</td>
 * <td>TORT023.DAT_FRA</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>yyyy.MM.dd / ingenting</td>
 * <td>Til og med-dato, siste dag i perioda avtaleproduktet strekker seg over</td>
 * <td>TORT023.DAT_TIL</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>2-sifra kode</td>
 * <td>Produktinfo</td>
 * <td>TORT023.KOD_ARBGIV_PROD_INFO</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>Desimaltall, 2 desimalar</td>
 * <td>Arbeidsgivarsats i prosent</td>
 * <td>TORT023.RTE_ARBGIV_PROD_ARBANDEL</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Desimaltall, 2 desimalar</td>
 * <td>Medlemssats i prosent</td>
 * <td>TORT023.RTE_ARBGIV_PROD_MEDL_ANDEL</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>Desimaltall, 2 desimalar</td>
 * <td>Administrasjonsgebyr i prosent</td>
 * <td>TORT023.RTE_ARBGIV_PROD_ADMGEB</td>
 * </tr>
 * <tr>
 * <td>9</td>
 * <td>Heiltall</td>
 * <td>Arbeidsgivarsats i prosent</td>
 * <td>TORT023.BEL_ARBGIV_PROD_ARBANDEL</td>
 * </tr>
 * <tr>
 * <td>10</td>
 * <td>Heiltall</td>
 * <td>Medlemssats i prosent</td>
 * <td>TORT023.BEL_ARBGIV_PROD_MEDL_ANDEL</td>
 * </tr>
 * <tr>
 * <td>11</td>
 * <td>Heiltall</td>
 * <td>Administrasjonsgebyr i prosent</td>
 * <td>TORT023.BEL_ARBGIV_PROD_ADMGEB</td>
 * </tr>
 * <tr>
 * <td>12</td>
 * <td>1- til 3-sifra kode</td>
 * <td>Risikoklasse (kun for YSK-produkt)</td>
 * <td>TORT023.TYP_ARBGIV_RISIKO_KL</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class AvtaleproduktOversetter extends ReflectiveCsvOversetter<AvtaleProduktFraCsv, Avtaleprodukt> implements CsvOversetter<Avtaleprodukt> {

    public AvtaleproduktOversetter() {
        super("AVTALEPRODUKT", AvtaleProduktFraCsv.class);
    }

    @Override
    protected Avtaleprodukt transformer(final AvtaleProduktFraCsv csvRad) {
        Prosent arbeidsgiverpremieProsent = csvRad.arbeidsgiverpremieProsent.map(Prosent::prosent).get();
        Prosent medlemspremieProsent = csvRad.medlemspremieProsent.map(Prosent::prosent).get();
        Prosent administrasjonsgebyrProsent = csvRad.administrasjonsgebyrProsent.map(Prosent::prosent).get();

        Optional<Satser<?>> prosentsatser = Stream.of(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent)
                .filter(p -> !p.equals(Prosent.ZERO, 3))
                .findFirst()
                .map(p -> new Satser<>(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent));

        Kroner arbeidsgiverpremieBeloep = csvRad.arbeidsgiverpremieBeloep.map(this::kroner).get();
        Kroner medlemspremieBeloep = csvRad.medlemspremieBeloep.map(this::kroner).get();
        Kroner administrasjonsgebyrBeloep = csvRad.administrasjonsgebyrBeloep.map(this::kroner).get();

        Optional<Satser<?>> kronesatser = Stream.of(arbeidsgiverpremieBeloep, medlemspremieBeloep, administrasjonsgebyrBeloep)
                .filter(k -> k.verdi() != 0)
                .findFirst()
                .map(p -> new Satser<>(arbeidsgiverpremieBeloep, medlemspremieBeloep, administrasjonsgebyrBeloep));

        if (kronesatser.isPresent() && prosentsatser.isPresent()) {
            throw new IllegalStateException("Både prosentsatser og kronesatser kan ikke være i bruk for et avtaleprodukt.");
        }

        Satser<?> satser = Stream.of(kronesatser, prosentsatser)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(Satser.ingenSatser());

        final Produkt produkt = csvRad.produkt.map(Produkt::fraKode).get();
        return new Avtaleprodukt(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.avtaleId.map(AvtaleId::valueOf).get(),
                produkt,
                csvRad.produktInfo.map(p -> new Produktinfo(Integer.parseInt(p))).get(),
                satser
        ).risikoklasse(csvRad.risikoklasse.filter(k -> produkt == Produkt.YSK).map(Risikoklasse::new));
    }

    public Kroner kroner(final String beloepString) {
        double beloep = Double.parseDouble(beloepString);
        if (beloep == 0) {
            return Kroner.ZERO;
        }
        return new Kroner(beloep);
    }
}
