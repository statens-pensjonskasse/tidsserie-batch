package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.medlemsdata;
import static no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import no.spk.tidsserie.batch.core.medlem.Medlemslinje;

import org.assertj.core.api.OptionalAssert;
import org.junit.jupiter.api.Test;

class UploaderTest {
    private final Partisjonstabell partisjonstabell = new Partisjonstabell();

    private final Uploader uploader = new Uploader(partisjonstabell);

    @Test
    void skal_akkumulere_medlemsdata_fram_til_opplasting_skal_utførast() {
        uploader.append(new Medlemslinje(rad("Adam", "Født", "2003")));
        uploader.append(new Medlemslinje(rad("Adam", "Eksisterer i", "Tidslinje A, Tidslinje B")));

        assertMedlemsdata("Adam").isEmpty();

        uploader.run();

        assertMedlemsdata("Adam")
                .contains(
                        medlemsdata(
                                rad("Født", "2003"),
                                rad("Eksisterer i", "Tidslinje A, Tidslinje B")
                        )
                );
    }

    @Test
    void skal_ikkje_akkumulere_medlemsdata_på_tvers_av_forskjellige_medlemmar() {
        uploader.append(new Medlemslinje(rad("Adam", "Født", "2003")));
        uploader.run();

        uploader.append(new Medlemslinje(rad("Agnes Nielsen", "Født", "1910")));
        uploader.run();

        assertMedlemsdata("Adam").contains(medlemsdata(rad("Født", "2003")));
        assertMedlemsdata("Agnes Nielsen").contains(medlemsdata(rad("Født", "1910")));
    }

    @Test
    void skal_eksplodere_om_opplasting_blir_gjort_uten_medlemsdata() {
        assertThatCode(uploader::run).isInstanceOf(OpplastingAvMedlemsdataKreverMinst1RadException.class);
    }

    @Test
    void skal_eksplodere_om_medlemsdata_inneheld_forskjellige_medlemsidar() {
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
}