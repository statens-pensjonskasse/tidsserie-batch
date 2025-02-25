package no.spk.tidsserie.batch.plugins.disruptor;

import no.spk.tidsserie.batch.core.lagring.Tidsserierad;

import com.lmax.disruptor.EventFactory;

class TidsserieradFactory implements EventFactory<Tidsserierad> {
    @Override
    public Tidsserierad newInstance() {
        return new Tidsserierad();
    }
}