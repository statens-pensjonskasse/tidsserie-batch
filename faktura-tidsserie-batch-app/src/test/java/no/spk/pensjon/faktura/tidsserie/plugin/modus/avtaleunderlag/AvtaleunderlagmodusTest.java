package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.util.Optional.empty;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
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

        services.registrer(Observasjonsperiode.class, new Observasjonsperiode(dato("2015.01.01"), dato("2015.01.01")));
        services.registrer(GrunnlagsdataService.class, mock(GrunnlagsdataService.class));
        services.registrer(StorageBackend.class, mock(StorageBackend.class));
        services.registry().registerService(Path.class, Paths.get("grunnlagsdata_2016-01-01_01-01-01-01"), Katalog.GRUNNLAGSDATA.egenskap());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void skal_lagre_avtaleunderlag() throws Exception {
        services.registrer(TidsperiodeFactory.class, mock(TidsperiodeFactory.class));
        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        modus.lagTidsserie(services.registry());

        verify(skriver).lagreUnderlag(any(Stream.class));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void skal_returnere_antall_avtaler_prosessert() throws Exception {
        PeriodeTypeTestFactory tidsperiodeFactory = new PeriodeTypeTestFactory();
        services.registrer(TidsperiodeFactory.class, tidsperiodeFactory);

        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        tidsperiodeFactory.addPerioder(
                enAvtalepriode(AvtaleId.avtaleId(1L)),
                enAvtalepriode(AvtaleId.avtaleId(2L))
        );
        final Map<String, Integer> resultat = modus.lagTidsserie(services.registry());

        assertThat(resultat.get("avtaler")).isEqualTo(2);
    }

    private Avtaleperiode enAvtalepriode(AvtaleId avtaleId) {
        return new Avtaleperiode(dato("2015.01.01"),
                empty(),
                avtaleId,
                ArbeidsgiverId.valueOf(2),
                empty());
    }
}