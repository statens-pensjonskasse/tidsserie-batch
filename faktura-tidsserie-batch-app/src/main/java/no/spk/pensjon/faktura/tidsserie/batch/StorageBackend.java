package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.storage.disruptor.ObservasjonsEvent;

/**
 * {@link StorageBackend} representerer lagringssystemet som tidsserieobservasjonane blir lagra via.
 * <br>
 * Alle lagringsformata tidsserien blir lagra til er pr dags dato tekst-baserte, ergo blir alle observasjonar
 * forventa å bli lagra som tekst via ein <code>StringBuilder</code> pr rad som backenden skal ta i mot og lagre.
 * <br>
 * For å hindre at prosesseringstrådane som ber om å lagre unna observasjonar skal bli neveverdig påvirka av forsinkelsen
 * som ein kvar form for I/O vil medføre, blir det forventa at alle implementasjonar av {@link StorageBackend} utfører
 * lagringa asynkront frå andre trådar enn trådane {@link #lagre(Consumer)} blir kalla frå.
 *
 * @author Tarjei Skorgenes
 */
public interface StorageBackend {
    /**
     * Lagrar innholdet som konsumenten populerer <code>eventen</code>en med.
     * <br>
     * Det er ei sterk forventning om at all lagring skje asynkront slik at I/O-latency ikkje kan påvirke
     * prosesseringstrådane som kallar denne metoda.
     * <br>
     * I situasjonar der backenden midlertidig eller permanent er ute av stand til å holde følge med prosesseringsrata,
     * vil denne metoda kunne blokkere den kallande tråden for å gi backenden tid til å hente seg inn igjen.
     * <br>
     * Ingen feil som <code>consumer</code>en kan komme til å kaste, vil bli fanga opp av backenden, alle feil vil
     * boble ut til den kallande tråden umiddelbart.
     *
     * @param consumer konsument som er ansvarlig for å populere eventen som skal lagrast
     * @throws RuntimeException viss <code>consumer</code> kastar ein {@link RuntimeException}
     * @throws Error            viss <code>consumer</code> kaster ein {@link Error}
     */
    void lagre(final Consumer<ObservasjonsEvent> consumer);
}