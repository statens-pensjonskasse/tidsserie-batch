package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;

import org.junit.Test;

public class AvtaleperiodeOversetterTest {
    private final AvtaleperiodeOversetter oversetter = new AvtaleperiodeOversetter();

    /**
     * Verifiserer at oversetteren fungerer for et forventet riktig csv-format
     */
    @Test
    public void skalParseCsvPaaRettFormat() {
        Avtaleperiode periode = oversetter.oversett(asList("AVTALE;123;1942-03-01;1945-03-01;1;555".split(";")) );
        assertThat(periode.fraOgMed()).isEqualTo(dato("1942.03.01"));
        assertThat(periode.tilOgMed()).isEqualTo(Optional.of(dato("1945.03.01")));
        assertThat(periode.avtale().id()).isEqualTo(123);
        assertThat(periode.arbeidsgiverId().id()).isEqualTo(555);
    }
}