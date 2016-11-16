package no.spk.felles.tidsserie.batch.storage.disruptor;

import no.spk.felles.tidsserie.batch.core.ObservasjonsEvent;

import com.lmax.disruptor.EventFactory;

class ObservasjonsEventFactory implements EventFactory<ObservasjonsEvent> {
    @Override
    public ObservasjonsEvent newInstance() {
        return new ObservasjonsEvent();
    }
}