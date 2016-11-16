package no.spk.pensjon.faktura.tidsserie.batch.storage.disruptor;

import java.io.IOException;

import no.spk.felles.tidsserie.batch.core.ObservasjonsEvent;

import com.lmax.disruptor.EventHandler;

/**
 *
 *
 * For event har tilknytta eit serienummer som kan benyttast av {@link ObservasjonsConsumer}-implementasjonar
 * til rute eventar på ein slik måte at eventar tilhøyrande samme serie blir samlokaliserte og liggande i samme
 * rekkefølge som dei vart mottatt frå klienten i. Dette er ønskelig for eksempel for å sikre at eventar tilhøyrande
 * samme stillingsforhold blir liggande i samme fil i kronologisk rekkefølge for live-tidsserien då det forenklar
 * feilsøking radikalt.
 *
 * @see ObservasjonsEvent
 */
interface ObservasjonsConsumer extends EventHandler<ObservasjonsEvent> {
    void close() throws IOException;
}