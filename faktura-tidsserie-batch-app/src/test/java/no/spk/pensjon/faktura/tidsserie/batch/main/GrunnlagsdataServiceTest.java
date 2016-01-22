package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.core.MedlemsdataUploader;
import no.spk.pensjon.faktura.tidsserie.core.Medlemslinje;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.GenerellTidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

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
    public final TemporaryFolder folder = new TemporaryFolderWithDeleteVerification();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();

    @Mock
    private TidsserieBackendService backend;

    @Mock
    private MedlemsdataUploader uploader;

    @Mock
    private GrunnlagsdataRepository repository;

    private GrunnlagsdataService service;

    @Before
    public void _before() throws IOException {
        service = new GrunnlagsdataService();
        when(repository.medlemsdata()).thenReturn(Stream.<List<String>>empty());
        when(repository.referansedata()).thenReturn(Stream.empty());
        when(backend.uploader()).thenReturn(uploader);

        registry.registrer(TidsserieBackendService.class, backend);
        registry.registrer(GrunnlagsdataRepository.class, repository);
    }

    @Test
    public void skalHaMedlemsdataOversetterForAlleStoettaMedlemsdatatyper() {
        assertThat(service.medlemsdataOversettere())
                .containsKeys(Stillingsendring.class, Avtalekoblingsperiode.class, Medregningsperiode.class);
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

        lastOpp();

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

        lastOpp();

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

        service.lesInnReferansedata(repository);

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

        lastOpp();

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

    private void lastOpp() {
        service.lastOpp(registry.registry());
    }
}