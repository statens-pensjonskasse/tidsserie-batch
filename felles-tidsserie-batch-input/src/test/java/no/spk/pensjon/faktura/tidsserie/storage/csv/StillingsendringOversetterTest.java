package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

public class StillingsendringOversetterTest {
    private final StillingsendringOversetter oversetter = new StillingsendringOversetter();

    /**
     * Verifiserer at dersom funksjonstillegg er tom, kun inneheld whitespace eller er lik 0
     * så blir det tolka som at ein ikkje har funksjonstillegg.
     */
    @Test
    public void skalTolkeTomtFunksjonstilleggEllerLikKroner0SomHarIkkjeFunksjonstillegg() {
        assertThat(oversetter.readFunksjonstillegg(Optional.of("0")))
                .as("stillingsendringas funksjonstillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFunksjonstillegg(empty()))
                .as("stillingsendringas funksjonstillegg")
                .isEqualTo(empty());
    }


    /**
     * Verifiserer at dersom variable tillegg er tom, kun inneheld whitespace eller er lik 0
     * så blir det tolka som at ein ikkje har faste tillegg.
     */
    @Test
    public void skalTolkeTommeVariabletilleggEllerLikKroner0SomHarIkkjeVariableTillegg() {
        assertThat(oversetter.readVariabletillegg(Optional.of("0")))
                .as("stillingsendringas variable tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readVariabletillegg(empty()))
                .as("stillingsendringas variable tillegg")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at dersom faste tillegg er tom, kun inneheld whitespace eller er lik 0
     * så blir det tolka som at ein ikkje har faste tillegg.
     */
    @Test
    public void skalTolkeTommeFastetilleggEllerLikKroner0SomHarIkkjeFasteTillegg() {
        assertThat(oversetter.readFastetillegg(Optional.of("0")))
                .as("stillingsendringas faste tillegg")
                .isEqualTo(empty());

        assertThat(oversetter.readFastetillegg(empty()))
                .as("stillingsendringas faste tillegg")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at dersom lønnstrinn er tom, kun inneheld whitespace eller er lik 0
     * så blir det tolka som at ein ikkje har lønnstrinn.
     */
    @Test
    public void skalTolkeLoennstrinn0SomAtStillingaIkkjeHarLoennstrinn() {
        assertThat(oversetter.readLoennstrinn(Optional.of("0")))
                .as("stillingsendringas lønnstrinn")
                .isEqualTo(empty());

        assertThat(empty())
                .as("stillingsendringas lønnstrinn")
                .isEqualTo(empty());
    }

    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD HH:mm:ss.S blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoSomInkludererTid() {
        assertThat(
                oversetter.tilDato(Optional.of("1942-03-01 00:00:00.0"))
        ).isEqualTo(of(dato("1942.03.01")));

        assertThat(
                oversetter.tilDato(Optional.of("1942-03-01 00:00:00.01"))
        ).isEqualTo(of(dato("1942.03.01")));

        assertThat(
                oversetter.tilDato(Optional.of("1942-03-01 00:00:00.012"))
        ).isEqualTo(of(dato("1942.03.01")));
    }


    /**
     * Verifiserer at oversetteren ikkje feilar dersom sybase datoar på formata
     * YYYY-MM-DD blir brukt som verdi på start- eller
     * sluttdatoane til avtalekoblingane.
     */
    @Test
    public void skalIkkjeFeilePaaSybaseDatoUtenTid() {
        assertThat(
                oversetter.tilDato(Optional.of("1942-03-01"))
        ).isEqualTo(of(dato("1942.03.01")));
    }

    @Test
    public void skalIkkjeFeileDersomDatoVerdiErTom() {
        assertThat(
                oversetter.tilDato(empty())
        ).isEqualTo(empty());
    }
}