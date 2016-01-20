package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;


import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon.avtaleversjon;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent.prosent;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Risikoklasse;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagformatTest {

    private Avtaleunderlagformat format;
    private AvtaleunderlagFactory factory;
    private PeriodeTypeTestFactory tidsperiodeFactory;

    @Before
    public void setUp() throws Exception {
        format = new Avtaleunderlagformat();
        tidsperiodeFactory = new PeriodeTypeTestFactory();
        factory = new AvtaleunderlagFactory(tidsperiodeFactory, new AvtaleunderlagRegelsett());

    }

    @Test
    public void skal_returnere_verdier_for_alle_kolonner() throws Exception {
        final AvtaleId avtaleId = avtaleId(1L);
        final ArbeidsgiverId arbeidsgiverId = ArbeidsgiverId.valueOf(1234);
        final Orgnummer orgnummer = Orgnummer.valueOf("999888222");

        tidsperiodeFactory.addPerioder(
                new Avtaleperiode(dato("2015.01.01"), empty(), avtaleId, arbeidsgiverId, of(Ordning.SPK)),
                new Arbeidsgiverdataperiode(dato("2015.01.01"), empty(), orgnummer, arbeidsgiverId),
                avtaleversjon(avtaleId)
                        .fraOgMed(dato("2015.01.01"))
                        .premiestatus(Premiestatus.AAO_01)
                        .premiekategori(Premiekategori.FASTSATS)
                        .bygg(),
                new Avtaleprodukt(
                        dato("2015.01.01"), empty(), avtaleId, Produkt.PEN, new Produktinfo(11),
                        new Satser<>(prosent("1%"), prosent("10%"), prosent("100%"))
                ),
                new Avtaleprodukt(
                        dato("2015.01.01"), empty(), avtaleId, Produkt.AFP, new Produktinfo(22),
                        new Satser<>(prosent("2%"), prosent("20%"), prosent("200%"))
                ),
                new Avtaleprodukt(
                        dato("2015.01.01"), empty(), avtaleId, Produkt.TIP, new Produktinfo(33),
                        new Satser<>(prosent("3%"), prosent("30%"), prosent("300%"))
                ),
                new Avtaleprodukt(
                        dato("2015.01.01"), empty(), avtaleId, Produkt.GRU, Produktinfo.GRU_35,
                        new Satser<>(kroner(2), kroner(20), kroner(200))
                ),
                new Avtaleprodukt(
                        dato("2015.01.01"), empty(), avtaleId, Produkt.YSK, new Produktinfo(44),
                        new Satser<>(kroner(1), kroner(10), kroner(100))
                ).risikoklasse(of(new Risikoklasse("6")))

        );

        final List<Underlag> underlag = factory.lagAvtaleunderlag(
                new Observasjonsperiode(dato("2015.01.01"), dato("2015.12.31")),
                new Uttrekksdato(dato("2016.01.01"))
        ).collect(toList());

        final String result = underlag.stream()
                .map(u -> u
                        .stream()
                        .map(p -> format
                                .serialiser(u, p)
                                .map(Object::toString)
                                .collect(joining("|")))
                        .collect(joining("\n")))
                .collect(joining());

        final UUID uuid = underlag.stream().flatMap(Underlag::stream).map(Underlagsperiode::id).findAny().get();
        final Tidsserienummer tidsserienummer = underlag.stream().map(p -> p.annotasjonFor(Tidsserienummer.class)).findAny().get();

        assertThat(result).isEqualTo(
                "2015|2015-01-01|2015-12-31|" +
                        "1|2016-01-01|" +
                        uuid + "|" +
                        "0|" +
                        tidsserienummer + "|" +
                        "100.00000000|365|365|" +
                        "3010|1234|999888222|" +
                        "AAO-01|FAS|" +
                        "1|1.00|10.00|100.00|111.00|11|" +
                        "1|2.00|20.00|200.00|222.00|22|" +
                        "1|3.00|30.00|300.00|333.00|33|" +
                        "666.00|" +
                        "1|2|20|200|222|35|" +
                        "1|1|10|100|111|44|6"
        );

    }

    @Test
    public void skal_returnere_riktige_kolonnenavn() throws Exception {
        final List<String> kolonner = format.kolonnenavn().collect(toList());
        assertThat(kolonner).containsExactly(
                "premieaar",
                "fraOgMedDato",
                "tilOgMedDato",
                "avtale",
                "uttrekksdato",
                "uuid",
                "antallFeil",
                "tidsserienummer",

                "regel_aarsfaktor",
                "regel_aarslengde",
                "regel_antalldager",

                "ordning",
                "arbeidsgivernummer",
                "organisasjonsnummer",

                "premiestatus",
                "premiekategori",

                "produkt_PEN",
                "produkt_PEN_satsArbeidsgiver",
                "produkt_PEN_satsMedlem",
                "produkt_PEN_satsAdministrasjonsgebyr",
                "produkt_PEN_satsTotal",
                "produkt_PEN_produktinfo",

                "produkt_AFP",
                "produkt_AFP_satsArbeidsgiver",
                "produkt_AFP_satsMedlem",
                "produkt_AFP_satsAdministrasjonsgebyr",
                "produkt_AFP_satsTotal",
                "produkt_AFP_produktinfo",

                "produkt_TIP",
                "produkt_TIP_satsArbeidsgiver",
                "produkt_TIP_satsMedlem",
                "produkt_TIP_satsAdministrasjonsgebyr",
                "produkt_TIP_satsTotal",
                "produkt_TIP_produktinfo",

                "produkt_prosent_satsTotal",

                "produkt_GRU",
                "produkt_GRU_satsArbeidsgiver",
                "produkt_GRU_satsMedlem",
                "produkt_GRU_satsAdministrasjonsgebyr",
                "produkt_GRU_satsTotal",
                "produkt_GRU_produktinfo",

                "produkt_YSK",
                "produkt_YSK_satsArbeidsgiver",
                "produkt_YSK_satsMedlem",
                "produkt_YSK_satsAdministrasjonsgebyr",
                "produkt_YSK_satsTotal",
                "produkt_YSK_produktinfo",
                "produkt_YSK_risikoklasse"
        );

    }

}