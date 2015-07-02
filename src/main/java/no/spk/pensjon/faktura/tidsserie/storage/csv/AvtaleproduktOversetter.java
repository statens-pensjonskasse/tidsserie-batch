package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.Optional;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

public class AvtaleproduktOversetter extends ReflectiveCsvOversetter<AvtaleproduktCsv,Avtaleprodukt> implements CsvOversetter<Avtaleprodukt> {

    public AvtaleproduktOversetter() {
        super("AVTALEPRODUKT", AvtaleproduktCsv.class);
    }

    @Override
    protected Avtaleprodukt transformer(AvtaleproduktCsv csvRad) {
        Prosent arbeidsgiverpremieProsent = csvRad.arbeidsgiverpremieProsent.map(Prosent::prosent).get();
        Prosent medlemspremieProsent = csvRad.medlemspremieProsent.map(Prosent::prosent).get();
        Prosent administrasjonsgebyrProsent = csvRad.administrasjonsgebyrProsent.map(Prosent::prosent).get();

        Optional<Satser<?>> prosentsatser = Stream.of(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent)
                .filter(p -> p.isGreaterThan(Prosent.ZERO))
                .findFirst()
                .map(p -> new Satser<>(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent));

        Kroner arbeidsgiverpremieBeloep = csvRad.arbeidsgiverpremieBeloep.map(this::kroner).get();
        Kroner medlemspremieBeloep = csvRad.medlemspremieBeloep.map(this::kroner).get();
        Kroner administrasjonsgebyrBeloep = csvRad.administrasjonsgebyrBeloep.map(this::kroner).get();

        Optional<Satser<?>> kronesatser = Stream.of(arbeidsgiverpremieBeloep, medlemspremieBeloep, administrasjonsgebyrBeloep)
                .filter(k -> k.verdi() > 0)
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

        return new Avtaleprodukt(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.avtaleId.map(AvtaleId::valueOf).get(),
                csvRad.produkt.map(Produkt::fraKode).get(),
                csvRad.produktInfo.map(p -> new Produktinfo(Integer.parseInt(p))).get(),
                satser
        );
    }

    public Kroner kroner(final String beloepString) {
        double beloep = Double.parseDouble(beloepString);
        if (beloep == 0) {
            return Kroner.ZERO;
        }
        return new Kroner(beloep);
    }
}
