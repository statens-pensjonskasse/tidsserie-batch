package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.stream.Collectors.toMap;
import static no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer.partisjonsnummer;
import static no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.medlemsdata;
import static no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Nodenummer.nodenummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;

import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;

class PartisjonstabellTest {
    private final Partisjonstabell partisjonstabell = new Partisjonstabell();

    @Test
    void skal_fordele_medlemsdata_på_271_partisjonar_for_å_videreføre_samme_oppførsel_som_hazelcast_pluginet() {
        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 1))).hasSize(271);
    }

    @Test
    void skal_fordele_antall_partisjonar_på_noder_basert_på_enkel_modulo_hashing_ikkje_konsistent_hashing() {
        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 1))).hasSize(271);

        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 2))).hasSize(136);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(2, 2))).hasSize(135);

        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 3))).hasSize(91);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(2, 3))).hasSize(90);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(3, 3))).hasSize(90);

        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 4))).hasSize(68);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(2, 4))).hasSize(68);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(3, 4))).hasSize(68);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(4, 4))).hasSize(67);

        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(2, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(3, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(4, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(5, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(6, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(7, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(8, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(9, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(10, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(11, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(12, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(13, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(14, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(15, 16))).hasSize(17);
        assertThat(partisjonstabell.partisjonarFor(nodenummer(16, 16))).hasSize(16);
    }

    @Test
    void skal_aldri_fordele_samme_partisjon_til_fleire_noder() {
        IntStream.rangeClosed(1, 128).forEach(
                antallNoder ->
                    assertThat(
                            IntStream
                                    .rangeClosed(1, antallNoder)
                                    .mapToObj(nodenummer -> nodenummer(nodenummer, antallNoder))
                                    .map(partisjonstabell::partisjonarFor)
                                    .flatMap(Set::stream)
                                    .toList()
                    )
                            .as("alle partisjonar for alle noder (antall noder = %d)", antallNoder)
                            .doesNotHaveDuplicates()
        );
    }

    @Test
    void skal_tømme_backenden_for_data() {
        partisjonstabell.put("Adam", medlemsdata(rad("Født som", "Jonas Kahnwald")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjonstabell.clear();
        assertThat(partisjonstabell.partisjonarFor(nodenummer(1, 1))).hasSize(0);
    }

    @Test
    void skal_ta_vare_på_alle_medlemmar_som_blir_lagt_til_i_partisjonstabellen() {
        partisjonstabell.put(
                "Adam",
                medlemsdata(
                        rad("Motto", "Sic Mundus Creatus Est")
                ).medlemsdata(), new DefaultDatalagringStrategi()
        );
        partisjonstabell.put(
                "Eva",
                medlemsdata(
                        rad("Motto", "Erit Lux")
                ).medlemsdata(), new DefaultDatalagringStrategi()
        );

        assertThat(
                prPartisjonsnummer(
                        partisjonstabell
                                .partisjonarFor(nodenummer(1, 1))
                                .stream()
                                .filter(partisjon -> !partisjon.isEmpty())
                )
        )
                .hasSize(2)
                .hasEntrySatisfying(
                        partisjonsnummer(70),
                        actual -> assertMedlemsdata(actual).containsEntry(
                                "Adam",
                                Collections.singletonList(
                                        rad("Motto", "Sic Mundus Creatus Est")
                                )
                        )
                )
                .hasEntrySatisfying(
                        partisjonsnummer(201),
                        actual -> assertMedlemsdata(actual).containsEntry(
                                "Eva",
                                Collections.singletonList(
                                        rad("Motto", "Erit Lux")
                                )
                        )
                );
    }


    private MapAssert<String, List<List<String>>> assertMedlemsdata(final Partisjon actual) {
        final HashMap<String, List<List<String>>> medlemsdata = new HashMap<>();
        actual.forEach(medlemsdata::put);
        return assertThat(medlemsdata);
    }

    private static Map<Partisjonsnummer, Partisjon> prPartisjonsnummer(final Stream<Partisjon> partisjonar) {
        return
                partisjonar
                        .filter(p -> !p.isEmpty())
                        .collect(toMap(Partisjon::nummer, Function.identity()));
    }
}
