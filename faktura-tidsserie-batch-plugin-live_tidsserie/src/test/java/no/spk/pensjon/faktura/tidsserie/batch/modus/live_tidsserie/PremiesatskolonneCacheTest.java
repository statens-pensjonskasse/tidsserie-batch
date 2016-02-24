package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.stream.IntStream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class PremiesatskolonneCacheTest {

    private Underlagsperiode underlagsperiode;
    private Premiesatskolonner premiesatskolonner;

    @Before
    public void setUp() throws Exception {
        underlagsperiode = new UnderlagsperiodeBuilder()
                .fraOgMed(LocalDate.now().minusDays(1))
                .tilOgMed(LocalDate.now())
                .bygg();

        premiesatskolonner = spy(new Premiesatskolonner());
    }

    @Test
    public void skal_cache_dersom_map_kalles_flere_ganger_paa_samme_index_som_ikke_er_siste_index() throws Exception {
        PremiesatskolonneCache cache = new PremiesatskolonneCache(premiesatskolonner, Produkt.PEN);
        IntStream.range(0, 5).forEach(i -> cache.map(underlagsperiode, 0));
        verify(premiesatskolonner, times(1)).forProdukt(underlagsperiode, Produkt.PEN);
    }

    @Test
    public void skal_cache_dersom_map_kalles_flere_ganger_paa_forskjellig_index() throws Exception {
        PremiesatskolonneCache cache = new PremiesatskolonneCache(premiesatskolonner, Produkt.PEN);
        IntStream.range(0, 5).forEach(i -> cache.map(underlagsperiode, i));
        verify(premiesatskolonner, times(1)).forProdukt(underlagsperiode, Produkt.PEN);
    }

    @Test
    public void skal_toemme_cache_naar_siste_index_er_mappet() throws Exception {
        PremiesatskolonneCache cache = new PremiesatskolonneCache(premiesatskolonner, Produkt.PEN);
        IntStream.range(0, 5).forEach(i -> cache.map(underlagsperiode, 4));
        verify(premiesatskolonner, times(5)).forProdukt(underlagsperiode, Produkt.PEN);
    }
}