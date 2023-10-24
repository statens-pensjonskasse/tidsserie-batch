package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer.partisjonsnummer;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.medlemsdata;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                        medlemsdata(
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
                        medlemsdata(
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
                        medlemsdata(
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
                        medlemsdata(
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
                        medlemsdata(
                                rad("æøå")
                        )
                );
    }

    @Test
    public void skal_ikkje_støtte_semikolon_i_verdiar_som_blir_lasta_opp() {
        assertThatCode(
                () -> partisjon.put("Nok eit medlem", medlemsdata(rad(";A;B;C;")))
        )
                .isInstanceOf(SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException.class);
    }

    @Test
    public void skal_ikkje_støtte_linjeskift_i_verdiar_som_blir_lasta_opp() {
        assertThatCode(
                () -> partisjon.put("Nok eit medlem", medlemsdata(rad("\nHei\nDu\nDer\n")))
        )
                .isInstanceOf(LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException.class);
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