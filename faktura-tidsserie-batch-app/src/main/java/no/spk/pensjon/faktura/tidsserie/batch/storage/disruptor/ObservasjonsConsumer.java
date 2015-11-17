package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

import java.io.IOException;

import no.spk.pensjon.faktura.tidsserie.core.ObservasjonsEvent;

import com.lmax.disruptor.EventHandler;

/**
 *
 *
 * For event har tilknytta eit serienummer som kan benyttast av {@link ObservasjonsConsumer}-implementasjonar
 * til rute eventar p� ein slik m�te at eventar tilh�yrande samme serie blir samlokaliserte og liggande i samme
 * rekkef�lge som dei vart mottatt fr� klienten i. Dette er �nskelig for eksempel for � sikre at eventar tilh�yrande
 * samme stillingsforhold blir liggande i samme fil i kronologisk rekkef�lge for live-tidsserien d� det forenklar
 * feils�king radikalt.
 *
 * @see ObservasjonsEvent
 */
interface ObservasjonsConsumer extends EventHandler<ObservasjonsEvent> {
    void close() throws IOException;
}