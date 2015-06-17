package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StillingsendringOversetterTest {
    private final StillingsendringOversetter oversetter = new StillingsendringOversetter();

    /**
     * Verifiserer at dersom funksjonstillegg er tom, kun inneheld whitespace eller er lik 0
     * s� blir det tolka som at ein ikkje har funksjonstillegg.
     */
    @Test
    public void skalTolkeTomtFunksjonstilleggEllerLikKroner0SomHarIkkjeFunksjonstillegg() {
        assertThat(oversetter.readFunksjonstillegg(asList("0"), 0))
                .as("stillingsendringas funksjonstillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFunksjonstillegg(asList(""), 0))
                .as("stillingsendringas funksjonstillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFunksjonstillegg(asList(" "), 0))
                .as("stillingsendringas funksjonstillegg")
                .isEqualTo(empty());
    }


    /**
     * Verifiserer at dersom variable tillegg er tom, kun inneheld whitespace eller er lik 0
     * s� blir det tolka som at ein ikkje har faste tillegg.
     */
    @Test
    public void skalTolkeTommeVariabletilleggEllerLikKroner0SomHarIkkjeVariableTillegg() {
        assertThat(oversetter.readVariabletillegg(asList("0"), 0))
                .as("stillingsendringas variable tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readVariabletillegg(asList(""), 0))
                .as("stillingsendringas variable tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readVariabletillegg(asList(" "), 0))
                .as("stillingsendringas variable tillegg")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at dersom faste tillegg er tom, kun inneheld whitespace eller er lik 0
     * s� blir det tolka som at ein ikkje har faste tillegg.
     */
    @Test
    public void skalTolkeTommeFastetilleggEllerLikKroner0SomHarIkkjeFasteTillegg() {
        assertThat(oversetter.readFastetillegg(asList("0"), 0))
                .as("stillingsendringas faste tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFastetillegg(asList(""), 0))
                .as("stillingsendringas faste tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFastetillegg(asList(" "), 0))
                .as("stillingsendringas faste tillegg")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at dersom l�nnstrinn er tom, kun inneheld whitespace eller er lik 0
     * s� blir det tolka som at ein ikkje har l�nnstrinn.
     */
    @Test
    public void skalTolkeLoennstrinn0SomAtStillingaIkkjeHarLoennstrinn() {
        assertThat(oversetter.readLoennstrinn(asList("0"), 0))
                .as("stillingsendringas l�nnstrinn")
                .isEqualTo(empty());

        assertThat(oversetter.readLoennstrinn(asList(""), 0))
                .as("stillingsendringas l�nnstrinn")
                .isEqualTo(empty());

        assertThat(oversetter.readLoennstrinn(asList(" "), 0))
                .as("stillingsendringas l�nnstrinn")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar p� formata
     * YYYY-MM-DD HH:mm:ss.S blir brukt som verdi p� start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoSomInkludererTid() {
        assertThat(
                oversetter.readDato(asList("1942-03-01 00:00:00.0"), 0)
        ).isEqualTo(of(dato("1942.03.01")));

        assertThat(
                oversetter.readDato(asList("1942-03-01 00:00:00.01"), 0)
        ).isEqualTo(of(dato("1942.03.01")));

        assertThat(
                oversetter.readDato(asList("1942-03-01 00:00:00.012"), 0)
        ).isEqualTo(of(dato("1942.03.01")));
    }


    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar p� formata
     * YYYY-MM-DD blir brukt som verdi p� start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoUtenTid() {
        assertThat(
                oversetter.readDato(asList("1942-03-01"), 0)
        ).isEqualTo(of(dato("1942.03.01")));
    }

    @Test
    public void skalIkkjeFeileDersomDatoVerdiErTom() {
        assertThat(
                oversetter.readDato(asList(""), 0)
        ).isEqualTo(empty());

        assertThat(
                oversetter.readDato(asList(" "), 0)
        ).isEqualTo(empty());
    }
}