package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Kundedataperiode;

import org.junit.Test;

public class KundedataOversetterTest {
    private final KundedataOversetter oversetter = new KundedataOversetter();

    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD HH:mm:ss.S blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalParseCsvPaaRettFormat() {
        Kundedataperiode kundedataperiode = oversetter.oversett(asList("KUNDEDATA;1;921321231;1942-03-01;;1".split(";")) );
        assertThat(kundedataperiode.fraOgMed()).isEqualTo(dato("1942.03.01"));
        assertThat(kundedataperiode.tilOgMed()).isEqualTo(Optional.empty());
        assertThat(kundedataperiode.orgnummer().id()).isEqualTo(921321231);
    }
}