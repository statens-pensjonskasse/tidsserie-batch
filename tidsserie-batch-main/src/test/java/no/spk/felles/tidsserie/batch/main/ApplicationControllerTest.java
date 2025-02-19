package no.spk.felles.tidsserie.batch.main;

import static no.spk.felles.tidsserie.batch.core.registry.Ranking.ranking;
import static no.spk.felles.tidsserie.batch.core.registry.Ranking.standardRanking;
import static no.spk.felles.tidsserie.batch.main.ApplicationController.EXIT_ERROR;
import static no.spk.felles.tidsserie.batch.main.ApplicationController.EXIT_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import no.spk.faktura.input.BatchId;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.LastOppGrunnlagsdataKommando;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UttrekksValidator;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;
import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Snorre E. Brekke - Computas
 */
@ExtendWith(MockitoExtension.class)
public class ApplicationControllerTest {

    @RegisterExtension
    public final StandardOutputAndError console = new StandardOutputAndError();

    @RegisterExtension
    public final LogbackVerifier logback = new LogbackVerifier();

    @TempDir
    public File temp;

    @RegisterExtension
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock(name = "medlemsdata")
    private MedlemsdataBackend medlemsdata;

    private ApplicationController controller;

    @BeforeEach
    void setUp() throws Exception {
        registry.registrer(View.class, new ConsoleView());
        controller = new ApplicationController(registry.registry());
        controller.initialiserLogging(new BatchId("x", LocalDateTime.now()), newFolder(temp, "junit").toPath());
    }

    @Test
    void informerOmOppstart() {
        ProgramArguments programArguments = new ProgramArguments();
        controller.informerOmOppstart(programArguments);
        verifiserInformasjonsmelding("Tidsserie-batch startet ");
        verifiserInformasjonsmelding("Følgende programargumenter blir brukt: ");
    }

    @Test
    void validerGrunnlagsdata() {
        UttrekksValidator validator = mock(UttrekksValidator.class);
        registry.registrer(UttrekksValidator.class, validator);
        controller.validerGrunnlagsdata();
        verify(validator).validate();
        verifiserInformasjonsmelding("Validerer grunnlagsdata.");
    }

    @Test
    void skal_rekaste_opprinnelig_feil_ved_validering_av_grunnlagsdata() {
        final UttrekksValidator validator = mock(UttrekksValidator.class);
        registry.registrer(UttrekksValidator.class, validator);

        final UgyldigUttrekkException expected = new UgyldigUttrekkException("I WAN't BETTER GRUNNLAGSDATA");
        doThrow(expected).when(validator).validate();

        try {
            controller.validerGrunnlagsdata();
        } catch (final UgyldigUttrekkException e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Test
    void ryddOpp() throws Exception {
        DirectoryCleaner cleaner = mock(DirectoryCleaner.class);
        controller.ryddOpp(cleaner);
        verify(cleaner).deleteDirectories();
        verifiserInformasjonsmelding("Sletter gamle filer.");
    }

    @Test
    void ryddOppFeilet() throws Exception {
        DirectoryCleaner cleaner = mock(DirectoryCleaner.class);
        doThrow(new HousekeepingException("blabla")).when(cleaner).deleteDirectories();

        assertThatCode(
                () -> controller.ryddOpp(cleaner)
        )
                .isInstanceOf(HousekeepingException.class);

        verify(cleaner).deleteDirectories();
    }

    @Test
    void informerOmBruk() {
        BruksveiledningSkalVisesException e = mock(BruksveiledningSkalVisesException.class);
        controller.informerOmBruk(e);
        verify(e).bruksveiledning();
        assertThat(controller.exitCode()).isEqualTo(EXIT_SUCCESS);
    }

    @Test
    void informerOmUgyldigeArgumenter() {
        UgyldigKommandolinjeArgumentException e = mock(UgyldigKommandolinjeArgumentException.class);
        controller.informerOmUgyldigeArgumenter(e);
        verify(e).bruksveiledning();
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    void informerOmUkjentFeil() {
        controller.informerOmUkjentFeil(new RuntimeException());
        verifiserInformasjonsmelding("Tidsserie-batch feilet - se logfil for detaljer.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    void informerOmKorrupteGrunnlagsdata() {
        controller.informerOmKorrupteGrunnlagsdata(new UgyldigUttrekkException("Feil."));
        verifiserInformasjonsmelding("Grunnlagsdata i inn-katalogen er korrupte - avbryter kjøringen.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    void skal_delegere_tidsserie_generering_til_modus() {
        final Tidsseriemodus modus = mock(Tidsseriemodus.class, "modus");
        when(modus.navn()).thenReturn("modus");

        controller.lagTidsserie(registry.registry(), modus);

        verify(modus).lagTidsserie(registry.registry());
        verifiserInformasjonsmelding("Starter tidsserie-generering");
        verifiserInformasjonsmelding("Tidsseriegenerering fullført.");
    }

    @Test
    void skal_informere_om_at_backend_blir_starta_opp() {
        controller.startBackend();
        verifiserInformasjonsmelding("Starter server.");
    }

    @Test
    void skal_starte_opp_den_høgast_rangerte_medlemsdata_backenden() {
        final MedlemsdataBackend medlemsdata_b = mock(MedlemsdataBackend.class, "lav_rangert");
        registry.registrer(MedlemsdataBackend.class, medlemsdata_b, standardRanking().egenskap());

        registry.registrer(MedlemsdataBackend.class, medlemsdata, ranking(9999).egenskap());

        controller.startBackend();

        verify(medlemsdata, times(1)).start();
        verify(medlemsdata_b, never()).start();
    }

    @Test
    void skal_rekaste_feil_frå_oppstart_av_medlemsdatabackend() {
        final RuntimeException expected = new RuntimeException("La oss late som om ein horribel feil oppstod");
        doThrow(expected).when(medlemsdata).start();

        registry.registrer(MedlemsdataBackend.class, medlemsdata);

        assertThatCode(
                () -> controller.startBackend()
        )
                .as("feil frå ApplicationController.startBackend")
                .isSameAs(expected);
    }

    @Test
    void skal_kalle_lastOpp_for_alle_grunnlagsdata_uploader_services() {
        final LastOppGrunnlagsdataKommando uploader1 = mock(LastOppGrunnlagsdataKommando.class);
        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader1, standardRanking().egenskap());

        final LastOppGrunnlagsdataKommando uploader2 = mock(LastOppGrunnlagsdataKommando.class);
        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader2, ranking(10).egenskap());

        controller.lastOpp();

        verify(uploader1, times(1)).lastOpp(any());
        verify(uploader2, times(1)).lastOpp(any());
        verifiserInformasjonsmelding("Starter lasting av grunnlagsdata...");
        verifiserInformasjonsmelding("Grunnlagsdata lastet.");
    }

    @Test
    void skal_rekaste_feil_ved_opplasting() {
        final LastOppGrunnlagsdataKommando uploader = mock(LastOppGrunnlagsdataKommando.class);
        doThrow(new UncheckedIOException(new IOException("MY CSV TASTES FUNNAY"))).when(uploader).lastOpp(any());

        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader);

        assertThatCode(
                () -> controller.lastOpp()
        )
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("MY CSV TASTES FUNNAY")
        ;
    }

    @Test
    void skal_vise_riktig_antall_feil() {
        Map<String, Integer> meldinger = new HashMap<>();
        //noinspection StringOperationCanBeSimplified
        meldinger.put(new String("errors"), 12);
        lagerTidsserien("stillingsforholdunderlag", meldinger);
        console.assertStandardOutput().contains("Antall feil: 12");
    }

    @Test
    void skal_vise_riktig_antall_behandlede_avtaler() {
        Map<String, Integer> meldinger = new HashMap<>();
        meldinger.put("avtaler", 100);
        lagerTidsserien("avtaleunderlag", meldinger);
        console.assertStandardOutput().contains("Antall avtaler behandlet: 100");
    }

    @Test
    void skal_vise_riktig_antall_behandlede_medlemmer() {
        Map<String, Integer> meldinger = new HashMap<>();
        meldinger.put("medlem", 1000);
        lagerTidsserien("live_tidsserie", meldinger);
        console.assertStandardOutput().contains("Antall medlemmer behandlet: 1000");
    }

    @Test
    void skal_aktivere_alle_plugins_tilgjengelig_via_tjenesteregisteret() {
        final Plugin plugin_a = registrerPlugin("plugin_a");
        final Plugin plugin_b = registrerPlugin("plugin_b");

        controller.aktiverPlugins();

        verifiserAtPluginVartAktivert(plugin_a);
        verifiserAtPluginVartAktivert(plugin_b);
    }

    @Test
    void skal_avbryte_køyringa_dersom_aktivering_av_minst_1_plugin_feilar() {
        final Throwable førsteFeil = new RuntimeException("Eg feila først!");
        final Plugin plugin_a = registrerPluginSomFeilar("plugin_a", førsteFeil);

        final Throwable andreFeil = new NullPointerException("Sjå, eg feila også!");
        final Plugin plugin_b = registrerPluginSomFeilar("plugin_b", andreFeil);

        final Plugin plugin_c = registrerPlugin("plugin_c");

        assertThatCode(
                () -> controller.aktiverPlugins()
        )
                .as(
                        "Aktivering av plugins skal feile dersom aktivering av minst 1 plugin feilar"
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aktivering av 2 plugins feila.")
                .hasMessageContaining("Feilmeldingar frå aktiveringa:")
                .hasMessageContaining("- " + førsteFeil.getMessage())
                .hasMessageContaining("- " + andreFeil.getMessage())
                .hasSuppressedException(førsteFeil)
                .hasSuppressedException(andreFeil)
        ;

        verifiserAtPluginVartAktivert(plugin_a);
        verifiserAtPluginVartAktivert(plugin_b);
        verifiserAtPluginVartAktivert(plugin_c);
    }

    private Plugin registrerPluginSomFeilar(final String tittel, final Throwable e) {
        final Plugin plugin = registrerPlugin(tittel);
        doThrow(e).when(plugin).aktiver(any());
        return plugin;
    }

    private void verifiserAtPluginVartAktivert(final Plugin plugin_a) {
        verify(plugin_a).aktiver(eq(registry.registry()));
    }

    private Plugin registrerPlugin(final String tittel) {
        final Plugin plugin_a = mock(Plugin.class, tittel);
        registry.registrer(Plugin.class, plugin_a);
        return plugin_a;
    }

    private void lagerTidsserien(String modusnavn, Map<String, Integer> meldinger) {
        final Tidsseriemodus modus = mock(Tidsseriemodus.class, "modus");
        final ServiceRegistry register = this.registry.registry();

        when(modus.navn()).thenReturn(modusnavn);
        when(modus.lagTidsserie(register)).thenReturn(meldinger);

        controller.lagTidsserie(register, modus);
        verify(modus).lagTidsserie(register);

    }

    private void verifiserInformasjonsmelding(String expectedMessage) {
        console.assertStandardOutput().contains(expectedMessage);
        logback.assertMessagesAsString().contains(expectedMessage);
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}