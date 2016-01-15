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
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.StandardOutputAndError;
import no.spk.pensjon.faktura.tidsserie.batch.upload.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

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

    ApplicationController controller;
    ConsoleView view;

    @Before
    public void setUp() throws Exception {
        view = new ConsoleView();
        controller = new ApplicationController(view);
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
        controller.validerGrunnlagsdata(validator);
        verify(validator).validate();
        console.assertStandardOutput().contains("Validerer grunnlagsdata.");
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
        TidsserieBackendService backend = mock(TidsserieBackendService.class);
        GrunnlagsdataService overfoering = mock(GrunnlagsdataService.class);
        Aarstall aarstall = new Aarstall(2007);

        controller.startBackend(backend);
        controller.lastOpp(overfoering);
        controller.lagTidsserie(backend, new Observasjonsperiode(dato("1970.01.01"), dato("1980.12.31")));

        verify(backend).start();
        verify(overfoering).lastOpp();
        verify(backend).lagTidsserie();

        console.assertStandardOutput().contains("Starter server.");
        console.assertStandardOutput().contains("Starter lasting av grunnlagsdata...");
        console.assertStandardOutput().contains("Grunnlagsdata lastet.");
        console.assertStandardOutput().contains("Starter tidsserie-generering");
        console.assertStandardOutput().contains("Tidsseriegenerering fullført.");
    }
}