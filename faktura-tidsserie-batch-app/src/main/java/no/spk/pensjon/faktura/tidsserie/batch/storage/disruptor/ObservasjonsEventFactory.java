package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

import no.spk.pensjon.faktura.tidsserie.core.ObservasjonsEvent;

import com.lmax.disruptor.EventFactory;

class ObservasjonsEventFactory implements EventFactory<ObservasjonsEvent> {
    @Override
    public ObservasjonsEvent newInstance() {
        return new ObservasjonsEvent();
    }
}