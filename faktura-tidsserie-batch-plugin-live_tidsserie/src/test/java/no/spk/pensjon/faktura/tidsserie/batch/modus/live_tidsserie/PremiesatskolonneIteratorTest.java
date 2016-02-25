package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static java.util.Optional.empty;
import static java.util.stream.IntStream.range;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class PremiesatskolonneIteratorTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Underlagsperiode underlagsperiode;
    private Premiesatskolonner premiesatskolonner;

    private int antallPremiesatsverdier;

    @Before
    public void setUp() throws Exception {
        underlagsperiode = nyPeriode();
        antallPremiesatsverdier = (int) new Premiesatskolonner().forPremiesats(underlagsperiode, empty()).count();
        premiesatskolonner = spy(new Premiesatskolonner());
    }

    private Underlagsperiode nyPeriode() {
        return new UnderlagsperiodeBuilder()
                .fraOgMed(LocalDate.now().minusDays(1))
                .tilOgMed(LocalDate.now())
                .bygg();
    }

    @Test
    public void skal_cache_verdier_for_produkt() throws Exception {
        PremiesatskolonneIterator cache = new PremiesatskolonneIterator(premiesatskolonner, Produkt.PEN);
        range(0, antallPremiesatsverdier).forEach(i -> cache.nestePremiesatsverdiFor(underlagsperiode));
        verify(premiesatskolonner, times(1)).forPremiesats(any(), any());
    }

    @Test
    public void skal_oppdatere_cachen_naar_alle_premiesatsverdier_er_konsumert_og_ny_periode_behandles() throws Exception {
        PremiesatskolonneIterator cache = new PremiesatskolonneIterator(premiesatskolonner, Produkt.PEN);
        range(0, antallPremiesatsverdier).forEach(i -> cache.nestePremiesatsverdiFor(underlagsperiode));
        cache.nestePremiesatsverdiFor(nyPeriode());
        verify(premiesatskolonner, times(2)).forPremiesats(any(), any());
    }

    @Test
    public void skal_feile_dersom_periode_endres_foer_alle_premiesatsverdier_er_konsumert() throws Exception {
        PremiesatskolonneIterator cache = new PremiesatskolonneIterator(premiesatskolonner, Produkt.PEN);

        cache.nestePremiesatsverdiFor(underlagsperiode);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("En ny underlagsperiode ble forsøkt behandlet, før samtlige premiesatskolonner for gjeldende underlagperiode var konsumert.");

        cache.nestePremiesatsverdiFor(nyPeriode());
    }
}