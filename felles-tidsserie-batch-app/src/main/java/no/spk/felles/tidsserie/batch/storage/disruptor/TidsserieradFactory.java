package no.spk.felles.tidsserie.batch.storage.disruptor;

import no.spk.felles.tidsserie.batch.core.lagring.Tidsserierad;

import com.lmax.disruptor.EventFactory;

class TidsserieradFactory implements EventFactory<Tidsserierad> {
    @Override
    public Tidsserierad newInstance() {
        return new Tidsserierad();
    }
}