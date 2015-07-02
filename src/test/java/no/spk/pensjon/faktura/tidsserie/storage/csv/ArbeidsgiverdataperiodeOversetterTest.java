package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;

import org.junit.Test;

public class ArbeidsgiverdataperiodeOversetterTest {
    private final ArbeidsgiverdataperiodeOversetter oversetter = new ArbeidsgiverdataperiodeOversetter();

    /**
     * Verifiserer at oversetteren fungerer for et forventet riktig csv-format
     */
    @Test
    public void skalParseCsvPaaRettFormat() {
        Arbeidsgiverdataperiode arbeidsgiverdataperiode = oversetter.oversett(asList("KUNDEDATA;1;921321231;1942-03-01;;1".split(";")) );
        assertThat(arbeidsgiverdataperiode.fraOgMed()).isEqualTo(dato("1942.03.01"));
        assertThat(arbeidsgiverdataperiode.tilOgMed()).isEqualTo(Optional.empty());
        assertThat(arbeidsgiverdataperiode.orgnummer().id()).isEqualTo(921321231);
    }
}