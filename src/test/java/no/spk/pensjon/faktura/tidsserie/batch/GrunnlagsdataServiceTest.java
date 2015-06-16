package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.GenerellTidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class GrunnlagsdataServiceTest {
    public static final List<String> MEDLEMSDATA = asList("1970010112345", "0", "19700101", "12345", "1122334455");

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private TidsserieBackendService backend;

    @Mock
    private MedlemsdataUploader uploader;

    @Mock
    private GrunnlagsdataRepository repository;

    private GrunnlagsdataService service;

    @Before
    public void _before() throws IOException {
        service = new GrunnlagsdataService(backend, repository);
        when(repository.medlemsdata()).thenReturn(Stream.<List<String>>empty());
        when(repository.referansedata()).thenReturn(Stream.empty());
        when(backend.uploader()).thenReturn(uploader);
    }

    /**
     * Verifiserer at backenden blir notifisert via uploaderen om at opplasting av alle data
     * er fullført.
     */
    @Test
    public void skalNotifisereBackendenOmAtLastingaErFullfoert() {
        service.lastOpp();
        verify(uploader).registrer(service);
    }

    /**
     * Verifiserer at alle medlemsdata blir lasta opp, inkludert siste rad.
     */
    @Test
    public void skalLasteOppAlleLinjerFraMedlemsdatafila() {
        when(repository.medlemsdata()).thenReturn(
                asList(
                        MEDLEMSDATA,
                        MEDLEMSDATA,
                        MEDLEMSDATA,
                        MEDLEMSDATA
                ).stream()
        );

        service.lastOpp();

        final ArgumentCaptor<Medlemslinje> captor = forClass(Medlemslinje.class);
        verify(uploader, times(4)).append(captor.capture());

        assertThat(captor.getAllValues())
                .as("opplasta medlemslinjer")
                .hasSize(4);
    }

    /**
     * Verifiserer at alle linjer tilhøyrande eit og samme medlem blir lasta opp sammen, ikkje ein gang pr linje.
     */
    @Test
    public void skalLasteOppEinGangPrMedlem() {
        when(repository.medlemsdata()).thenReturn(
                asList(
                        MEDLEMSDATA,
                        MEDLEMSDATA,
                        asList(
                                "1980010112345", "0", "19800101", "12345", "2233445566"
                        ),
                        asList(
                                "1980010112345", "0", "19800101", "12345", "2233445566"
                        )
                ).stream()
        );

        service.lastOpp();

        verify(uploader, times(2)).run();
    }

    /**
     * Verifiserer at straumen med referansedata blir automatisk lukka etter behandling for å sikre at eventuelle
     * åpne filer blir lukka.
     */
    @Test
    public void skalLukkeReferansedataEtterBruk() {
        final Runnable closer = mock(Runnable.class);
        final Stream<Tidsperiode<?>> referansedata = Stream
                .<Tidsperiode<?>>empty()
                .onClose(closer);
        when(repository.referansedata()).thenReturn(referansedata);

        service.lesInnReferansedata();

        verify(closer).run();
    }

    /**
     * Verifiserer at straumen med medlemsdata blir automatisk lukka etter behandling for å sikre at eventuelle
     * åpne filer blir lukka.
     */
    @Test
    public void skalLukkeMedlemsdataEtterBruk() throws IOException {
        final Runnable closer = mock(Runnable.class);
        final Stream<List<String>> medlemsdata = Stream
                .<List<String>>empty()
                .onClose(closer);
        when(repository.medlemsdata()).thenReturn(medlemsdata);

        service.lastOpp();

        verify(closer).run();
    }

    /**
     * Verifiserer at {@link GrunnlagsdataService#perioderAvType} returnerer ei tom liste slik at vi unngår
     * NPE dersom det manglar data for ein eller fleire av datatypene som opptrer som referansedata i tidsserien.
     * <br>
     * Intensjonen med dette er å la dette problemet bli validert elsewhere og unngå ein ekkel og uhandterbar NPE i
     * loggane til batchen.
     */
    @Test
    public void skalReturnereTomListeDersomIngenTidsperioderAvAngittTypeHarBlittLestInn() {
        assertThat(service.perioderAvType(GenerellTidsperiode.class).count())
                .as("antall perioder av type " + GenerellTidsperiode.class)
                .isEqualTo(0);
    }

    @Test
    public void skalLukkeInputfiler() throws IOException {
        when(backend.uploader()).thenReturn(uploader);
        when(repository.medlemsdata()).thenReturn(singletonList(MEDLEMSDATA).stream());

        service.lastOpp();

        verify(uploader).registrer(service);
    }
}