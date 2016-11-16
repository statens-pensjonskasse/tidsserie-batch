package no.spk.pensjon.faktura.tidsserie.batch.main;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_ERROR;
import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import no.spk.faktura.input.BatchId;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.felles.tidsserie.batch.core.LastOppGrunnlagsdataKommando;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;
import no.spk.felles.tidsserie.batch.main.input.StandardOutputAndError;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.Constants;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ApplicationControllerTest {
    @Rule
    public final StandardOutputAndError console = new StandardOutputAndError();

    @Rule
    public final LogbackVerifier logback = new LogbackVerifier();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    ApplicationController controller;

    @Before
    public void setUp() throws Exception {
        registry.registrer(View.class, new ConsoleView());
        controller = new ApplicationController(registry.registry());
        controller.initialiserLogging(new BatchId("x", LocalDateTime.now()), temp.newFolder().toPath());
    }

    @Test
    public void testInformerOmOppstart() throws Exception {
        ProgramArguments programArguments = new ProgramArguments();
        controller.informerOmOppstart(programArguments);
        verifiserInformasjonsmelding("Tidsserie-batch startet ");
        verifiserInformasjonsmelding("Følgende programargumenter blir brukt: ");
    }

    @Test
    public void testValiderGrunnlagsdata() throws Exception {
        GrunnlagsdataDirectoryValidator validator = mock(GrunnlagsdataDirectoryValidator.class);
        registry.registrer(GrunnlagsdataDirectoryValidator.class, validator);
        controller.validerGrunnlagsdata();
        verify(validator).validate();
        verifiserInformasjonsmelding("Validerer grunnlagsdata.");
    }

    @Test
    public void skal_rekaste_opprinnelig_feil_ved_validering_av_grunnlagsdata() {
        final GrunnlagsdataDirectoryValidator validator = mock(GrunnlagsdataDirectoryValidator.class);
        registry.registrer(GrunnlagsdataDirectoryValidator.class, validator);

        final GrunnlagsdataException expected = new GrunnlagsdataException("I WAN't BETTER GRUNNLAGSDATA");
        doThrow(expected).when(validator).validate();

        try {
            controller.validerGrunnlagsdata();
        } catch (final GrunnlagsdataException e) {
            assertThat(e).isSameAs(expected);
        }
    }

    @Test
    public void testRyddOpp() throws Exception {
        DirectoryCleaner cleaner = mock(DirectoryCleaner.class);
        controller.ryddOpp(cleaner);
        verify(cleaner).deleteDirectories();
        verifiserInformasjonsmelding("Sletter gamle filer.");
    }

    @Test
    public void testRyddOppFeilet() throws Exception {
        DirectoryCleaner cleaner = mock(DirectoryCleaner.class);
        doThrow(new HousekeepingException("blabla")).when(cleaner).deleteDirectories();
        exception.expect(HousekeepingException.class);

        controller.ryddOpp(cleaner);
        verify(cleaner).deleteDirectories();
    }

    @Test
    public void testInformerOmBruk() throws Exception {
        UsageRequestedException mockException = mock(UsageRequestedException.class);
        controller.informerOmBruk(mockException);
        verify(mockException).usage();
        assertThat(controller.exitCode()).isEqualTo(EXIT_SUCCESS);
    }

    @Test
    public void testInformerOmUgyldigeArgumenter() throws Exception {
        InvalidParameterException mockException = mock(InvalidParameterException.class);
        controller.informerOmUgyldigeArgumenter(mockException);
        verify(mockException).usage();
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    public void testInformerOmUkjentFeil() throws Exception {
        controller.informerOmUkjentFeil(new RuntimeException());
        verifiserInformasjonsmelding("Tidsserie-batch feilet - se logfil for detaljer.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    public void testInformerOmKorrupteGrunnlagsdata() throws Exception {
        controller.informerOmKorrupteGrunnlagsdata(new GrunnlagsdataException("Feil."));
        verifiserInformasjonsmelding("Grunnlagsdata i inn-katalogen er korrupte - avbryter kjøringen.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    public void skal_delegere_tidsserie_generering_til_modus() {
        final Tidsseriemodus modus = mock(Tidsseriemodus.class, "modus");
        when(modus.navn()).thenReturn("modus");

        controller.lagTidsserie(registry.registry(), modus, new Observasjonsperiode(dato("1970.01.01"), dato("1980.12.31")));

        verify(modus).lagTidsserie(registry.registry());
        verifiserInformasjonsmelding("Starter tidsserie-generering");
        verifiserInformasjonsmelding("Tidsseriegenerering fullført.");
    }

    @Test
    public void skal_starte_backend() {
        final MedlemsdataBackend backend = mock(MedlemsdataBackend.class);
        controller.startBackend(backend);

        verify(backend).start();
        verifiserInformasjonsmelding("Starter server.");
    }

    @Test
    public void skal_kalle_standard_grunnlagsdata_uploader_service() throws IOException {
        final LastOppGrunnlagsdataKommando uploader1 = mock(LastOppGrunnlagsdataKommando.class);
        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader1, Constants.SERVICE_RANKING + "=0");

        final LastOppGrunnlagsdataKommando uploader2 = mock(LastOppGrunnlagsdataKommando.class);
        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader2, Constants.SERVICE_RANKING + "=10");

        controller.lastOpp();

        verify(uploader1, times(0)).lastOpp(any());
        verify(uploader2, times(1)).lastOpp(any());
        verifiserInformasjonsmelding("Starter lasting av grunnlagsdata...");
        verifiserInformasjonsmelding("Grunnlagsdata lastet.");
    }

    @Test
    public void skal_rekaste_feil_ved_opplasting() {
        exception.expect(UncheckedIOException.class);
        exception.expectMessage("MY CSV TASTES FUNNAY");

        final LastOppGrunnlagsdataKommando uploader = mock(LastOppGrunnlagsdataKommando.class);
        doThrow(new UncheckedIOException(new IOException("MY CSV TASTES FUNNAY"))).when(uploader).lastOpp(any());

        registry.registrer(LastOppGrunnlagsdataKommando.class, uploader);

        controller.lastOpp();
    }

    @Test
    public void skal_vise_riktig_antall_feil() {
        Map<String, Integer> meldinger = new HashMap<>();
        meldinger.put(new String("errors"), 12);
        lagerTidsserien("stillingsforholdunderlag", meldinger);
        console.assertStandardOutput().contains("Antall feil: 12");
    }

    @Test
    public void skal_vise_riktig_antall_behandlede_avtaler() {
        Map<String, Integer> meldinger = new HashMap<>();
        meldinger.put("avtaler", 100);
        lagerTidsserien("avtaleunderlag", meldinger);
        console.assertStandardOutput().contains("Antall avtaler behandlet: 100");
    }

    @Test
    public void skal_vise_riktig_antall_behandlede_medlemmer() {
        Map<String, Integer> meldinger = new HashMap<>();
        meldinger.put("medlem", 1000);
        lagerTidsserien("live_tidsserie", meldinger);
        console.assertStandardOutput().contains("Antall medlemmer behandlet: 1000");
    }

    private void lagerTidsserien(String modusnavn, Map<String, Integer> meldinger) {
        final Tidsseriemodus modus = mock(Tidsseriemodus.class, "modus");
        final ServiceRegistry register = this.registry.registry();

        when(modus.navn()).thenReturn(modusnavn);
        when(modus.lagTidsserie(register)).thenReturn(meldinger);

        controller.lagTidsserie(register, modus, new Observasjonsperiode(dato("1970.01.01"), dato("1980.12.31")));
        verify(modus).lagTidsserie(register);

    }

    private void verifiserInformasjonsmelding(String expectedMessage) {
        console.assertStandardOutput().contains(expectedMessage);
        logback.assertMessagesAsString().contains(expectedMessage);
    }
}