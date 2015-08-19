package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import java.io.IOException;

import com.lmax.disruptor.EventHandler;

interface ObservasjonsConsumer extends EventHandler<ObservasjonsEvent> {
    void close() throws IOException;
}