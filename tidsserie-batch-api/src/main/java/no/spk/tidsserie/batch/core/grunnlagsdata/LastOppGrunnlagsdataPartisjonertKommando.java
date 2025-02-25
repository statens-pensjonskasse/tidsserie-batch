package no.spk.tidsserie.batch.core.grunnlagsdata;

import java.io.UncheckedIOException;

import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link LastOppGrunnlagsdataPartisjonertKommando} lar modusane plugge inn tenester som har
 * behov for å lese inn grunnlagsdata, typisk via {@link GrunnlagsdataRepository}
 * ved oppstart av batchen, før nokon av {@link TidsserieLivssyklus} blir kalla og
 * tidsseriegenereringa blir initiert.
 *
 * @author Tarjei Skorgenes
 * @see GrunnlagsdataRepository
 */
public interface LastOppGrunnlagsdataPartisjonertKommando {
    /**
     * Notifiserer tenesta om at batchen har satt opp {@link GrunnlagsdataRepository} og
     * dermed er klar for innlesing eller {@link MedlemsdataBackend#uploader() opplasting} av desse.
     *
     * @param serviceRegistry tjenesteregisteret som {@link GrunnlagsdataRepository} og eventuelle andre samarbeidande tenester
     * kan hentast frå
     * @throws UncheckedIOException dersom det oppstår eit problem med lesinga frå disk
     */
    void lastOpp(final ServiceRegistry serviceRegistry, final long partisjonsnummer) throws UncheckedIOException;
}
