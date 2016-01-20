package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
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

    @SuppressWarnings("unchecked")
    @Test
    public void skal_lage_avtaleunderlag() throws Exception {
        services.registrer(Observasjonsperiode.class, new Observasjonsperiode(dato("2015.01.01"), dato("2015.01.01")));
        services.registrer(GrunnlagsdataService.class, mock(GrunnlagsdataService.class));
        services.registrer(StorageBackend.class, mock(StorageBackend.class));
        services.registrer(TidsperiodeFactory.class, mock(TidsperiodeFactory.class));
        services.registry().registerService(Path.class, Paths.get("grunnlagsdata_2016-01-01_01-01-01-01"), Katalog.GRUNNLAGSDATA.egenskap());

        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        modus.lagTidsserie(services.registry());

        verify(skriver).lagreUnderlag(any(Stream.class));
    }
}