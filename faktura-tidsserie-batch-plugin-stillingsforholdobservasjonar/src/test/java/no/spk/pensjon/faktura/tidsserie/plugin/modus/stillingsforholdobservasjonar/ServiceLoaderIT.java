package no.spk.pensjon.faktura.tidsserie.plugin.modus.stillingsforholdobservasjonar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.ServiceLoader;

import no.spk.pensjon.faktura.tidsserie.batch.core.Tidsseriemodus;

import org.junit.Test;

public class ServiceLoaderIT {
    @Test
    public void skal_vere_auto_detekterbar_via_service_loader() {
        final ArrayList<Tidsseriemodus> modusar = new ArrayList<>();
        ServiceLoader.load(Tidsseriemodus.class).forEach(modusar::add);

        assertThat(modusar)
                .as("tidsseriemodusar tilgjengelig via ServiceLoader")
                .hasSize(1);
        assertThat(modusar.get(0)).isInstanceOf(Stillingsforholdprognosemodus.class);
    }
}
