package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.storage.disruptor.ObservasjonsEvent;

/**
 * {@link StorageBackend} representerer lagringssystemet som tidsserieobservasjonane blir lagra via.
 * <br>
 * Alle lagringsformata tidsserien blir lagra til er pr dags dato tekst-baserte, ergo blir alle observasjonar
 * forventa � bli lagra som tekst via ein <code>StringBuilder</code> pr rad som backenden skal ta i mot og lagre.
 * <br>
 * For � hindre at prosesseringstr�dane som ber om � lagre unna observasjonar skal bli neveverdig p�virka av forsinkelsen
 * som ein kvar form for I/O vil medf�re, blir det forventa at alle implementasjonar av {@link StorageBackend} utf�rer
 * lagringa asynkront fr� andre tr�dar enn tr�dane {@link #lagre(Consumer)} blir kalla fr�.
 *
 * @author Tarjei Skorgenes
 */
public interface StorageBackend {
    /**
     * Lagrar innholdet som konsumenten populerer <code>eventen</code>en med.
     * <br>
     * Det er ei sterk forventning om at all lagring skje asynkront slik at I/O-latency ikkje kan p�virke
     * prosesseringstr�dane som kallar denne metoda.
     * <br>
     * I situasjonar der backenden midlertidig eller permanent er ute av stand til � holde f�lge med prosesseringsrata,
     * vil denne metoda kunne blokkere den kallande tr�den for � gi backenden tid til � hente seg inn igjen.
     * <br>
     * Ingen feil som <code>consumer</code>en kan komme til � kaste, vil bli fanga opp av backenden, alle feil vil
     * boble ut til den kallande tr�den umiddelbart.
     *
     * @param consumer konsument som er ansvarlig for � populere eventen som skal lagrast
     * @throws RuntimeException viss <code>consumer</code> kastar ein {@link RuntimeException}
     * @throws Error            viss <code>consumer</code> kaster ein {@link Error}
     */
    void lagre(final Consumer<ObservasjonsEvent> consumer);
}