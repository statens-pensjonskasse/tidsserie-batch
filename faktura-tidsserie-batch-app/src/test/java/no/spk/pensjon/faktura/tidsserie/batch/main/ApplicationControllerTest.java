package no.spk.pensjon.faktura.tidsserie.batch.main;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_ERROR;
import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.StandardOutputAndError;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;
import no.spk.pensjon.faktura.tjenesteregister.support.SimpleServiceRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ApplicationControllerTest {
    @Rule
    public final StandardOutputAndError console = new StandardOutputAndError();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    ApplicationController controller;

    @Before
    public void setUp() throws Exception {
        registry.registrer(View.class, new ConsoleView());
        controller = new ApplicationController(registry.registry());
    }

    @Test
    public void testInformerOmOppstart() throws Exception {
        ProgramArguments programArguments = new ProgramArguments();
        controller.informerOmOppstart(programArguments);
        console.assertStandardOutput().contains("Tidsserie-batch startet ");
        console.assertStandardOutput().contains("Følgende programargumenter blir brukt: ");
    }

    @Test
    public void testValiderGrunnlagsdata() throws Exception {
        GrunnlagsdataDirectoryValidator validator = mock(GrunnlagsdataDirectoryValidator.class);
        registry.registrer(GrunnlagsdataDirectoryValidator.class, validator);
        controller.validerGrunnlagsdata();
        verify(validator).validate();
        console.assertStandardOutput().contains("Validerer grunnlagsdata.");
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
        console.assertStandardOutput().contains("Sletter gamle filer.");
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
        console.assertStandardOutput().contains("Tidsserie-batch feilet - se logfil for detaljer.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    public void testInformerOmKorrupteGrunnlagsdata() throws Exception {
        controller.informerOmKorrupteGrunnlagsdata(new GrunnlagsdataException("Feil."));
        console.assertStandardOutput().contains("Grunnlagsdata i inn-katalogen er korrupte - avbryter kjøringen.");
        assertThat(controller.exitCode()).isEqualTo(EXIT_ERROR);
    }

    @Test
    public void testTidsserieGenerering() throws Exception {
        ServiceRegistry registry = new SimpleServiceRegistry();
        TidsserieBackendService backend = mock(TidsserieBackendService.class);
        Tidsseriemodus modus = mock(Tidsseriemodus.class);
        GrunnlagsdataService overfoering = mock(GrunnlagsdataService.class);

        controller.startBackend(backend);
        controller.lastOpp(overfoering);
        controller.lagTidsserie(registry, modus, new Observasjonsperiode(dato("1970.01.01"), dato("1980.12.31")));

        verify(backend).start();
        verify(overfoering).lastOpp();
        verify(modus).lagTidsserie(registry);

        console.assertStandardOutput().contains("Starter server.");
        console.assertStandardOutput().contains("Starter lasting av grunnlagsdata...");
        console.assertStandardOutput().contains("Grunnlagsdata lastet.");
        console.assertStandardOutput().contains("Starter tidsserie-generering");
        console.assertStandardOutput().contains("Tidsseriegenerering fullført.");
    }
}