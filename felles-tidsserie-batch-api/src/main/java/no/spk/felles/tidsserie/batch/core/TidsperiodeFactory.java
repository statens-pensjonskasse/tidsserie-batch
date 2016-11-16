package no.spk.felles.tidsserie.batch.core;

import java.util.stream.Stream;

import no.spk.felles.tidsperiode.Tidsperiode;

/**
 * {@link TidsperiodeFactory} gir tilgang til alle tidsperioder frå grunnlagsdatane batchen har tilgang til.
 *
 * @author Tarjei Skorgenes
 * @since 1.2.0
 */
public interface TidsperiodeFactory {
    /**
     * Hentar ut alle tidsperiodiserte lønnsdata som ikkje er medlemsspesifikke.
     *
     * @return alle lønnsdata
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperioder
     */
    Stream<Tidsperiode<?>> loennsdata();

    /**
     * Hentar ut alle tidsperioder av ei bestemt type
     *
     * @param <T>  periodetypa som skal hentast ut
     * @param type periodetypa som skal hentast ut
     * @return alle tidsperioder av type <code>type</code>
     */
    <T> Stream<T> perioderAvType(Class<T> type);
}
