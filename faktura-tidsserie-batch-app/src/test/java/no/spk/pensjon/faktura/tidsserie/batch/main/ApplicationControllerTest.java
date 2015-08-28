package no.spk.pensjon.faktura.tidsserie.batch.main;

import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_ERROR;
import static no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController.EXIT_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Paths;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.InvalidParameterException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArgumentsFactory.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.StandardOutputAndError;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

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
        console.assertStandardOutput().contains("Grunnlagsdata-batch startet ");
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
        FileTemplate malFilnavn = new FileTemplate(Paths.get("."), "prefix", "postfix");
        Aarstall aarstall = new Aarstall(2007);

        controller.startBackend(backend);
        controller.lastOpp(overfoering);
        controller.lagTidsserie(backend, malFilnavn, aarstall, aarstall);

        verify(backend).start();
        verify(overfoering).lastOpp();
        verify(backend).lagTidsseriePaaStillingsforholdNivaa(malFilnavn, aarstall, aarstall);

        console.assertStandardOutput().contains("Starter server.");
        console.assertStandardOutput().contains("Starter lasting av grunnlagsdata...");
        console.assertStandardOutput().contains("Grunnlagsdata lastet.");
        console.assertStandardOutput().contains("Starter tidsserie-generering");
        console.assertStandardOutput().contains("Tidsseriegenerering fullført.");
    }
}