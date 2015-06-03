package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Optional.ofNullable;
import static no.spk.pensjon.faktura.tidsserie.storage.csv.Feilmeldingar.ugyldigAntallKolonnerForAvtaleprodukt;

import java.util.List;
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

public class AvtaleproduktOversetter {
    public static final String TYPEINDIKATOR = "AVTALEPRODUKT";

    public static final int INDEX_AVTALE = 1;
    public static final int INDEX_PRODUKT = 2;
    public static final int INDEX_FRA_OG_MED_DATO = 3;
    public static final int INDEX_TIL_OG_MED_DATO = 4;
    public static final int INDEX_PRODUKTINFO = 5;
    public static final int INDEX_ARBEIDSGIVERPREMIE_PROSENT = 6;
    public static final int INDEX_MEDLEMSPREMIE_PROSENT = 7;
    public static final int INDEX_ADMINISTRASJONSGEBYR_PROSENT = 8;
    public static final int INDEX_ARBEIDSGIVERPREMIE_BELOEP = 9;
    public static final int INDEX_MEDLEMSPREMIE_BELOEP = 10;
    public static final int INDEX_ADMINISTRASJONSGEBYR_BELOEP = 11;

    private static final int ANTALL_KOLONNER = INDEX_ADMINISTRASJONSGEBYR_PROSENT + 1;

    public boolean supports(final List<String> rad) {
        return TYPEINDIKATOR.equals(rad.get(0));
    }

    public Avtaleprodukt oversett(final List<String> rad) {
        if (rad.size() < ANTALL_KOLONNER) {
            throw new IllegalArgumentException(
                    ugyldigAntallKolonnerForAvtaleprodukt(rad)
            );
        }

        Prosent arbeidsgiverpremieProsent = read(rad, INDEX_ARBEIDSGIVERPREMIE_PROSENT).map(Prosent::prosent).get();
        Prosent medlemspremieProsent = read(rad, INDEX_MEDLEMSPREMIE_PROSENT).map(Prosent::prosent).get();
        Prosent administrasjonsgebyrProsent = read(rad, INDEX_ADMINISTRASJONSGEBYR_PROSENT).map(Prosent::prosent).get();

        Optional<Satser<?>> prosentsatser = Stream.of(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent)
                .filter(p -> p.isGreaterThan(Prosent.ZERO))
                .findFirst()
                .map(p -> new Satser<>(arbeidsgiverpremieProsent, medlemspremieProsent, administrasjonsgebyrProsent));

        Kroner arbeidsgiverpremieBeloep = read(rad, INDEX_ARBEIDSGIVERPREMIE_BELOEP).map(this::kroner).get();
        Kroner medlemspremieBeloep = read(rad, INDEX_MEDLEMSPREMIE_BELOEP).map(this::kroner).get();
        Kroner administrasjonsgebyrBeloep = read(rad, INDEX_ADMINISTRASJONSGEBYR_BELOEP).map(this::kroner).get();

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
                read(rad, INDEX_FRA_OG_MED_DATO).map(Datoar::dato).get(),
                read(rad, INDEX_TIL_OG_MED_DATO).map(Datoar::dato),
                read(rad, INDEX_AVTALE).map(AvtaleId::valueOf).get(),
                read(rad, INDEX_PRODUKT).map(Produkt::fraKode).get(),
                read(rad, INDEX_PRODUKTINFO).map(p -> new Produktinfo(Integer.parseInt(p))).get(),
                satser
        );
    }

    private Optional<String> read(final List<String> rad, final int index) {
        return ofNullable(rad.get(index)).map(String::trim).filter(t -> !t.isEmpty());
    }

    public Kroner kroner(final String beloepString) {
        double beloep = Double.parseDouble(beloepString);
        if (beloep == 0) {
            return Kroner.ZERO;
        }
        return new Kroner(beloep);
    }
}
