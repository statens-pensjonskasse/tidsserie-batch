package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AvtalekoblingOversetterTest {
    private final AvtalekoblingOversetter oversetter = new AvtalekoblingOversetter();

    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD HH:mm:ss.S blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoSomInkludererTid() {
        assertThat(
                oversetter.oversett(
                        asList(
                                "1;54321012;54321;7654321;1942-03-01 00:00:00.0;;223344;3010".split(";")
                        )
                ).fraOgMed()
        ).isEqualTo(dato("1942.03.01"));

        assertThat(
                oversetter.oversett(
                        asList(
                                "1;54321012;54321;7654321;1942-03-01 00:00:00.0;2010-09-16 00:00:00.012;223344;3010".split(";")
                        )
                ).tilOgMed()
        ).isEqualTo(of(dato("2010.09.16")));
    }


    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoUtenTid() {
        assertThat(
                oversetter.oversett(
                        asList(
                                "1;54321012;54321;7654321;1942-03-01;;223344;3010".split(";")
                        )
                ).fraOgMed()
        ).isEqualTo(dato("1942.03.01"));

        assertThat(
                oversetter.oversett(
                        asList(
                                "1;54321012;54321;7654321;1942-03-01;2010-09-16;223344;3010".split(";")
                        )
                ).tilOgMed()
        ).isEqualTo(of(dato("2010.09.16")));
    }
}