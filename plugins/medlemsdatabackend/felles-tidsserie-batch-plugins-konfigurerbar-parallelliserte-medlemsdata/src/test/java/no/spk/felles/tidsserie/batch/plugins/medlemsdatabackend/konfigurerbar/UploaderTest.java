package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.List;

import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;

import org.assertj.core.api.OptionalAssert;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class UploaderTest {
    private final Partisjonstabell partisjonstabell = new Partisjonstabell();

    private final Uploader uploader = new Uploader(partisjonstabell, new DefaultDatalagringStrategi());

    @Test
    public void skal_akkumulere_medlemsdata_fram_til_opplasting_skal_utførast() {
        uploader.append(new Medlemslinje(rad("Adam", "Født", "2003")));
        uploader.append(new Medlemslinje(rad("Adam", "Eksisterer i", "Tidslinje A, Tidslinje B")));

        assertMedlemsdata("Adam").isEmpty();

        uploader.run();

        assertMedlemsdata("Adam")
                .contains(
                        Lists.newArrayList(
                                rad("Født", "2003"),
                                rad("Eksisterer i", "Tidslinje A, Tidslinje B")
                        )
                );
    }

    @Test
    public void skal_ikkje_akkumulere_medlemsdata_på_tvers_av_forskjellige_medlemmar() {
        uploader.append(new Medlemslinje(rad("Adam", "Født", "2003")));
        uploader.run();

        uploader.append(new Medlemslinje(rad("Agnes Nielsen", "Født", "1910")));
        uploader.run();

        assertMedlemsdata("Adam").contains(Collections.singletonList(rad("Født", "2003")));
        assertMedlemsdata("Agnes Nielsen").contains(Collections.singletonList(rad("Født", "1910")));
    }

    @Test
    public void skal_eksplodere_om_opplasting_blir_gjort_uten_medlemsdata() {
        assertThatCode(uploader::run).isInstanceOf(OpplastingAvMedlemsdataKreverMinst1RadException.class);
    }

    @Test
    public void skal_eksplodere_om_medlemsdata_inneheld_forskjellige_medlemsidar() {
        uploader.append(new Medlemslinje(rad("Adam")));
        uploader.append(new Medlemslinje(rad("Eva")));
        assertThatCode(uploader::run).isInstanceOf(ForskjelligeMedlemmarForsoektLastaOppSammenException.class);
    }

    private OptionalAssert<List<List<String>>> assertMedlemsdata(final String medlemsId) {
        return assertThat(
                partisjonstabell.get(medlemsId)
        )
                .as("Partisjonstabell.medlemsdataFor(<%s>)", medlemsId);
    }

    @Test
    public void skal_ikkje_støtte_semikolon_i_verdiar_som_blir_lasta_opp() {
        uploader.append(new Medlemslinje(rad("medlem", ";A;B;C;")));
        assertThatCode(
                uploader::run
        )
                .isInstanceOf(SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException.class);
    }

    @Test
    public void skal_ikkje_støtte_linjeskift_i_verdiar_som_blir_lasta_opp() {
        uploader.append(new Medlemslinje(rad("medlem", "\nHei\nDu\nDer\n")));
        assertThatCode(
                uploader::run
        )
                .isInstanceOf(LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException.class);
    }
}