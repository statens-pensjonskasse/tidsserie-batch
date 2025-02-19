package no.spk.felles.tidsserie.batch.core.registry;

import static java.lang.String.format;
import static no.spk.pensjon.faktura.tjenesteregister.Constants.SERVICE_RANKING;

import no.spk.pensjon.faktura.tjenesteregister.Constants;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link Ranking} tilbyr funksjonalitet for å overstyre rankingen
 * ved {@link ServiceRegistry#registerService(Class, Object, String...) registrering}
 * av tenester i {@link ServiceRegistry tenesteregisteret}.
 *
 * @since 1.1.0
 */
public class Ranking {
    private final static Ranking DEFAULT = new Ranking(0);

    private final int ranking;

    private Ranking(final int ranking) {
        this.ranking = ranking;
    }

    public static Ranking ranking(final int verdi) {
        return new Ranking(verdi);
    }

    public static Ranking standardRanking() {
        return DEFAULT;
    }

    /**
     * Produserer ein tenesteegenskap som styrer rankingen på ei teneste som blir lagt inn i
     * tenesteregisteret.
     *
     * @return ein egenskap som tenesteregisteret skal benytte som tenesta sin ranking
     * @see Constants#SERVICE_RANKING
     * @see ServiceRegistry#registerService(Class, Object, String...)
     */
    public String egenskap() {
        return format("%s=%d", SERVICE_RANKING, ranking);
    }
}
