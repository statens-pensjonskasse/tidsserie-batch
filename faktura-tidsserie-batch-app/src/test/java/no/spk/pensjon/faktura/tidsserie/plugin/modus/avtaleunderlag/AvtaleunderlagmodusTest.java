package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagmodusTest {

    @Rule
    public ServiceRegistryRule services = new ServiceRegistryRule();

    private Avtaleunderlagmodus modus;

    @Before
    public void setUp() throws Exception {
        modus = new Avtaleunderlagmodus();
    }

    @Test
    public void skal_registrere_avtaleunderlagfactory() throws Exception {
        services.registrer(GrunnlagsdataService.class, mock(GrunnlagsdataService.class));
        modus.registerServices(services.registry());
        assertThat(services.registry().getServiceReference(AvtaleunderlagFactory.class)).isPresent();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skal_lage_avtaleunderlag() throws Exception {
        services.registrer(Observasjonsperiode.class, new Observasjonsperiode(dato("2015.01.01"), dato("2015.01.01")));
        services.registrer(GrunnlagsdataService.class, mock(GrunnlagsdataService.class));
        services.registrer(FileTemplate.class, mock(FileTemplate.class));
        services.registry().registerService(Path.class, Paths.get("grunnlagsdata_2016-01-01_01-01-01-01"), Katalog.GRUNNLAGSDATA.egenskap());
        modus.registerServices(services.registry());

        Avtaleunderlagskriver skriver = mock(Avtaleunderlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        modus.lagTidsserie(services.registry());

        verify(skriver).skrivAvtaleunderlag(any(Stream.class));
    }
}