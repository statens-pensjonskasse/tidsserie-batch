package no.spk.felles.tidsserie.batch.plugins.disruptor.gzipped;

import java.io.IOException;

import no.spk.felles.tidsserie.batch.core.lagring.Tidsserierad;

import com.lmax.disruptor.EventHandler;

/**
 * Tenester av type {@link TidsserieradHandler} står ansvarlig for å lagre {@link Tidsserierad tidsserierader}
 * til eit eller anna form for persistent lager.
 *
 * @see Tidsserierad
 */
interface TidsserieradHandler extends EventHandler<Tidsserierad> {
    /**
     * Notifiserer tenesta om at den ikkje lenger vil motta fleire rader for lagring.
     * <br>
     * Tenesta står dermed fritt til å rydde opp og lukke eventuelle nettverksforbindelsar eller
     * åpne filer.
     *
     * @throws IOException dersom ein I/O-relatert feil oppstår under opprydding og lukking av åpne ressursar
     */
    void close() throws IOException;
}