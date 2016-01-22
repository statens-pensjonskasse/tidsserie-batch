package no.spk.pensjon.faktura.tidsserie.batch.core;

import java.io.UncheckedIOException;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Laster opp grunnlagsdata frå {@link GrunnlagsdataRepository}  og gjer dei tilgjengelig via
 * {@link TidsserieBackendService} og {@link TidsperiodeFactory}.
 *
 * @author Tarjei Skorgenes
 */
public interface LastOppGrunnlagsdataKommando {
    /**
     * Lastar inn {@link GrunnlagsdataRepository#referansedata()} og
     * {@link GrunnlagsdataRepository#medlemsdata()} og lastar dei opp til backend-tenestene som gjer dei
     * tilgjengelig via {@link TidsserieBackendService} og {@link TidsperiodeFactory}.
     *
     * @param registry tjenesteregisteret som {@link GrunnlagsdataRepository} og eventuelle andre samarbeidande tenester
     * kan hentast frå
     * @throws UncheckedIOException dersom det oppstår eit problem med lesinga frå disk
     */
    void lastOpp(final ServiceRegistry registry) throws UncheckedIOException;
}
