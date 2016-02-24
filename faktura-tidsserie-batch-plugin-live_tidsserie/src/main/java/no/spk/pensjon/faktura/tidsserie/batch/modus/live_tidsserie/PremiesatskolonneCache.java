package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * @author Snorre E. Brekke - Computas
 */
class PremiesatskolonneCache {
    private final Premiesatskolonner premiesatskolonner;
    private final Produkt produkt;

    private List<Function<Underlagsperiode, String>> cache;

    PremiesatskolonneCache(Premiesatskolonner premiesatskolonner, Produkt produkt) {
        this.produkt = produkt;
        this.premiesatskolonner = premiesatskolonner;
    }

    String map(Underlagsperiode underlagsperiode, int index) {
        if (cache == null) {
            cache = premiesatskolonner.forProdukt(underlagsperiode, produkt).collect(Collectors.toList());
        }
        final String verdi = cache.get(index).apply(underlagsperiode);
        if (index == cache.size() - 1) {
            cache = null;
        }
        return verdi;
    }
}
