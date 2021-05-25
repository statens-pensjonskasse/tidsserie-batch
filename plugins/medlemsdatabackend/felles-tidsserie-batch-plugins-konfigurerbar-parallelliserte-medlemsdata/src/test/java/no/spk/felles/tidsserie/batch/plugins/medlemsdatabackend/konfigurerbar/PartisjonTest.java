package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.medlemsdata;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Partisjonsnummer.partisjonsnummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Test;

public class PartisjonTest {
    private final Partisjon partisjon = new Partisjon(partisjonsnummer(1));

    @Test
    public void skal_godta_opplasting_av_samme_medlem_fleire_gangar_og_appende_kvart_sett_med_medlemsdata_på_medlemmet_dei_tilhøyrer() {
        partisjon.put("Et medlem", medlemsdata(rad("A", "2")));
        partisjon.put("Ulikt medlem", medlemsdata(rad("X", "24")));
        partisjon.put("Et medlem", medlemsdata(rad("C", "3")));
        partisjon.put("Noen andre", medlemsdata(rad("Z", "Y")));
        partisjon.put("Et medlem", medlemsdata(rad("B", "1")));

        assertThat(
                hentMedlemsdata()
        )
                .containsEntry(
                        "Et medlem",
                        Lists.newArrayList(
                                rad("A", "2"),
                                rad("C", "3"),
                                rad("B", "1")
                        )
                );
    }

    @Test
    public void skal_bevare_antal_kolonner_inntakt_sjølv_om_kolonner_på_slutten_av_rada_er_tomme() {
        partisjon.put(
                "Et medlem",
                medlemsdata(
                        rad("A", "2", "", "", "")
                )
        );

        assertThat(
                hentMedlemsdata()
        )
                .containsEntry(
                        "Et medlem",
                        Collections.singletonList(
                                rad("A", "2", "", "", "")
                        )
                );
    }

    @Test
    public void skal_godta_variasjon_i_antall_kolonner() {
        partisjon.put(
                "Et medlem",
                medlemsdata(
                        rad("A"),
                        rad(),
                        rad("B", "1"),
                        rad(""),
                        rad("C", "1", "", "99")
                )
        );

        assertThat(
                hentMedlemsdata()
        )
                .containsEntry(
                        "Et medlem",
                        Lists.newArrayList(
                                rad("A"),
                                rad(""),
                                rad("B", "1"),
                                rad(""),
                                rad("C", "1", "", "99")
                        )
                );
    }

    @Test
    public void skal_konvertere_rader_uten_kolonner_til_rader_med_1_tom_kolonne() {
        partisjon.put(
                "Et medlem",
                medlemsdata(
                        rad()
                )
        );

        assertThat(
                hentMedlemsdata()
        )
                .containsEntry(
                        "Et medlem",
                        Collections.singletonList(
                                rad("")
                        )
                );
    }

    @Test
    public void skal_støtte_norske_tegn() {
        partisjon.put(
                "ÆØÅ",
                medlemsdata(
                        rad("æøå")
                )
        );

        assertThat(
                hentMedlemsdata()
        )
                .containsEntry(
                        "ÆØÅ",
                        Collections.singletonList(
                                rad("æøå")
                        )
                );
    }

    @Test
    public void skal_ha_menneskevennlig_toString() {
        partisjon.put("1", medlemsdata(rad()));
        partisjon.put("2", medlemsdata(rad()));

        assertThat(
                partisjon
        )
                .hasToString("partisjon 1 av 271 (2 medlemmar)");
    }

    private Map<String, List<List<String>>> hentMedlemsdata() {
        final HashMap<String, List<List<String>>> medlemsdata = new HashMap<>();
        partisjon.forEach(medlemsdata::put);
        return medlemsdata;
    }
}