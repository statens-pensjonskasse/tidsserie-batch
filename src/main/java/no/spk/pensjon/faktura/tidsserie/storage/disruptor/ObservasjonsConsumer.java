package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import com.lmax.disruptor.EventHandler;

import java.io.IOException;

interface ObservasjonsConsumer extends EventHandler<ObservasjonsEvent> {
    void close() throws IOException;
}