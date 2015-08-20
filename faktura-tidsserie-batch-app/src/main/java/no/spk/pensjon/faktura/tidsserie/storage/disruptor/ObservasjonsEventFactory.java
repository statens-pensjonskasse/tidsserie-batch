package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import com.lmax.disruptor.EventFactory;

class ObservasjonsEventFactory implements EventFactory<ObservasjonsEvent> {
    @Override
    public ObservasjonsEvent newInstance() {
        return new ObservasjonsEvent();
    }
}