package no.spk.pensjon.faktura.tidsserie.plugin.modus;


import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import no.spk.pensjon.faktura.tidsserie.core.ObservasjonsEvent;

import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class KolonnenavnPerPartisjonTest {

    @Test
    public void skal_sette_serienummer_paa_event() throws Exception {

        final ObservasjonsEvent event = new ObservasjonsEvent();

        KolonnenavnPerPartisjon initer = new KolonnenavnPerPartisjon(
                new ArrayList<>(),
                consumer -> consumer.accept(event));

        initer.partitionInitialized(1);

        assertThat(event.serienummer()).isPresent();
        assertThat(event.serienummer().get()).isEqualTo(1);
    }

    @Test
    public void skal_skrive_kolonnenavn() throws Exception {
        final ObservasjonsEvent event = new ObservasjonsEvent();

        KolonnenavnPerPartisjon initer = new KolonnenavnPerPartisjon(
                asList("kolonne", "annenKolonne"),
                consumer -> consumer.accept(event));

        initer.partitionInitialized(1);

        assertThat(event.buffer.toString()).isEqualTo("kolonne;annenKolonne\n");
    }
}