package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FormatSpesifikasjonTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    private final Desimaltallformatering desimalar = new Desimaltallformatering();

    @Test
    public void skal_verifisere_at_det_er_lagt_inn_minst_ei_kolonne() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("Spesifikasjonen må inneholde minst ei kolonne");

        new FormatSpesifikasjon(desimalar) {
            @Override
            protected void definer() {
            }
        };
    }

    @Test
    public void skal_verifisere_at_foerste_kolonne_har_kolonnenummer_1() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("Spesifikasjonen manglar ein definisjon for kolonnenummer 1");

        new FormatSpesifikasjon(desimalar) {
            @Override
            protected void definer() {
                kolonne(2, "myself", dummy());
                kolonne(3, "and irene", dummy());
            }
        };
    }

    @Test
    public void skal_verifisere_at_det_ikkje_er_lagt_inn_fleire_kolonner_med_samme_nummer() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("Spesifikasjonen kan ikkje inneholde fleire kolonner med samme kolonnenummer");
        e.expectMessage("2");

        new FormatSpesifikasjon(desimalar) {
            @Override
            protected void definer() {
                kolonne(1, "me", dummy());
                kolonne(2, "myself", dummy());
                kolonne(2, "and irene", dummy());
            }
        };
    }

    @Test
    public void skal_verifisere_at_det_ikkje_er_gap_mellom_kolonnene() {
        e.expect(IllegalArgumentException.class);
        e.expectMessage("Spesifikasjonen må inneholde ein definisjon for alle kolonnenummer mellom kolonne 1 og kolonne 4");
        e.expectMessage("kolonne 1 og 3");

        new FormatSpesifikasjon(desimalar) {
            @Override
            protected void definer() {
                kolonne(1, "me", dummy());
                kolonne(3, "myself", dummy());
                kolonne(4, "and irene", dummy());
            }
        };
    }

    @Test
    public void skal_sortere_kolonnene_paa_kolonnenummer() {
        final FormatSpesifikasjon spesifikasjon = new FormatSpesifikasjon(desimalar) {
            @Override
            protected void definer() {
                kolonne(3, "and irene", dummy());
                kolonne(1, "me", dummy());
                kolonne(2, "myself", dummy());
            }
        };
        assertThat(spesifikasjon.kolonnenavn().toArray()).containsExactly("me", "myself", "and irene");
    }

    private static FormatSpesifikasjon.KolonneMapper dummy() {
        return (u, up) -> "";
    }
}