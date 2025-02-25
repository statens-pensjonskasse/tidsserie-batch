package no.spk.tidsserie.batch.plugins.disruptor.gzipped;

import no.spk.tidsserie.batch.core.lagring.Tidsserierad;

import com.lmax.disruptor.EventFactory;

class TidsserieradFactory implements EventFactory<Tidsserierad> {
    @Override
    public Tidsserierad newInstance() {
        return new Tidsserierad();
    }
}