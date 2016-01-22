package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;


import static java.util.stream.Collectors.joining;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode.avtaleperiode;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.ObservasjonsEvent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;

import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class UnderlagskriverTest {
    @Test
    public void skal_skrive_underlag_til_storage() throws Exception {
        PeriodeTypeTestFactory tidsperiodeFactory = new PeriodeTypeTestFactory();
        AvtaleunderlagFactory factory = new AvtaleunderlagFactory(tidsperiodeFactory, new AvtaleunderlagRegelsett());

        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                avtaleperiode(avtaleId)
                        .fraOgMed(dato("2015.01.01"))
                        .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                        .bygg()
        );

        final Stream<Underlag> underlag = factory.lagAvtaleunderlag(
                new Observasjonsperiode(dato("2015.01.01"), dato("2015.12.31")),
                new Uttrekksdato(dato("2016.01.01"))
        );

        final Avtaleunderlagformat avtaleformat = new Avtaleunderlagformat();

        ObservasjonsEvent output = new ObservasjonsEvent();

        Underlagskriver skriver = new Underlagskriver(consumer -> consumer.accept(output), avtaleformat);
        skriver.lagreUnderlag(underlag);

        final String[] outputlinjer = output.buffer.toString().split("\n");
        assertThat(outputlinjer).hasSize(2);
        assertThat(outputlinjer[0]).isEqualTo(avtaleformat.kolonnenavn().collect(joining(";")));
        assertThat(outputlinjer[1]).startsWith("2015;2015-01-01;2015-12-31;1");
    }
}