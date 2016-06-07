package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

/**
 * Integrasjonstestsuite for avtaleunderlagets {@link AvtaleunderlagAvslutter}.
 *
 * @author Tarjei Skorgenes
 */
public class AvtaleunderlagAvslutterIT {
    private final TemporaryFolder temp = new TemporaryFolder();

    private final UtkatalogRule utkatalog = new UtkatalogRule(temp);

    @Rule
    public final RuleChain chain = RuleChain.outerRule(temp).around(utkatalog);

    @Test
    public void skal_lage_filliste_med_tidsserie_filnavn_som_innhold() throws IOException {
        final String expected = "tidsserie1-3861323e-1efc-4b8c-834e-5ed1af91a356.csv";
        utkatalog.write(expected, singletonList("1;2;3;4"));

        run();

        utkatalog.assertFillister().hasSize(1);
        utkatalog.assertFillisteInnhold().containsOnly(expected);
    }

    @Test
    public void skal_lage_kun_ei_filliste_sjoelv_om_tidsserien_skulle_ha_blitt_splitta_i_fleire_filer() throws IOException {
        final List<String> tidsserieFilnavn = asList(
                "tidsserie1-3861323e-1efc-4b8c-834e-5ed1af91a356.csv",
                "tidsserie2-2861323e-1efc-4b8c-834e-5ed1af91a356.csv",
                "tidsserie3-1861323e-1efc-4b8c-834e-5ed1af91a356.csv"
        );
        for (final String filnavn : tidsserieFilnavn) {
            utkatalog.write(filnavn, singletonList("1;2;3;4"));
        }

        run();

        utkatalog.assertFillister().hasSize(1);
        utkatalog.assertFillisteInnhold()
                .hasSize(3)
                .containsOnlyElementsOf(tidsserieFilnavn);
    }

    @Test
    public void skal_lage_tom_filliste_dersom_ingen_tidsseriefil_har_blitt_generert() throws IOException {
        run();

        utkatalog.assertFillister().hasSize(1);
        utkatalog.assertFillisteInnhold().isEmpty();
    }

    private void run() {
        new AvtaleunderlagAvslutter(utkatalog.ut()).tidsserieGenerert(null);
    }

}