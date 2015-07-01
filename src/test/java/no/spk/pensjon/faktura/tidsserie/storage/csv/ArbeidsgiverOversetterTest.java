package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverperiode;

import org.junit.Test;

public class ArbeidsgiverOversetterTest {
    private final ArbeidsgiverOversetter oversetter = new ArbeidsgiverOversetter();

    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD HH:mm:ss.S blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalParseCsvPaaRettFormat() {
        Arbeidsgiverperiode periode = oversetter.oversett(asList("AVTALE;123;1942-03-01;1945-03-01".split(";")) );
        assertThat(periode.fraOgMed()).isEqualTo(dato("1942.03.01"));
        assertThat(periode.tilOgMed()).isEqualTo(Optional.of(dato("1945.03.01")));
        assertThat(periode.arbeidsgiver().id()).isEqualTo(123);
    }
}