package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon.avregningsversjon;
import static org.assertj.core.api.Assertions.assertThat;

import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;

import org.junit.Test;

public class AvregningsperiodeOversetterTest {
    private final AvregningsperiodeOversetter oversetter = new AvregningsperiodeOversetter();

    @Test
    public void skal_kun_stoette_avregningsperioder() {
        assertThat(oversetter.supports(singletonList("0"))).isFalse();
        assertThat(oversetter.supports(singletonList("AVTALEVERSJON"))).isFalse();
        assertThat(oversetter.supports(singletonList("AVREGNINGSPERIODE"))).isTrue();
    }

    @Test
    public void skal_mappe_fra_og_med_dato_til_1_januar_i_premieaaret_fra_kolonne_2() {
        assertThat(
                oversetter
                        .oversett(
                                asList(
                                        "AVREGNINGSPERIODE",
                                        "2015",
                                        "2016",
                                        "1"
                                )
                        )
                        .fraOgMed()
        )
                .isEqualTo(dato("2015.01.01"));
    }

    @Test
    public void skal_mappe_til_og_med_dato_til_31_desember_i_premieaaret_fra_kolonne_3() {
        assertThat(
                oversetter
                        .oversett(
                                asList(
                                        "AVREGNINGSPERIODE",
                                        "2015",
                                        "2016",
                                        "1"
                                )
                        )
                        .tilOgMed()
        )
                .isEqualTo(of(dato("2016.12.31")));
    }

    @Test
    public void skal_mappe_avregningsversjon_fra_kolonne_4() {
        final Underlagsperiode periode = new Underlagsperiode(dato("2015.02.02"), dato("2015.02.02"));
        oversetter
                .oversett(
                        asList(
                                "AVREGNINGSPERIODE",
                                "2015",
                                "2016",
                                "189"
                        )
                )
                .annoter(periode);
        assertThat(periode.annotasjonFor(Avregningsversjon.class))
                .isEqualTo(avregningsversjon(189));
    }
}