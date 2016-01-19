package no.spk.pensjon.faktura.tidsserie.batch.it;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;

import cucumber.api.DataTable;
import cucumber.api.java8.No;

public class ModusDefinisjon implements No {
    public ModusDefinisjon() {
        Gitt("^at brukaren ønskjer å generere ein tidsserie$", () -> {
        });

        Så("^skal følgjande modusar vere tilgjengelige for bruk:$", (DataTable modusar) -> {
            final List<String> actual = modusar.transpose().topCells();
            actual.remove("Navn");
            assertThat(
                    Modus
                            .stream()
                            .map(Modus::kode)
                            .collect(toList())
            )
                    .containsOnlyElementsOf(
                            actual
                    )
            ;
        });
    }
}
