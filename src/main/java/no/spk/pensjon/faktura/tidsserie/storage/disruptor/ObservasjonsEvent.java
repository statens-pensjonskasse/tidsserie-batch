package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

/**
 * {@link ObservasjonsEvent} inneheld den serialiserte representasjonen av ein tidsserieobservasjon på
 * stillingsforholdnivå.
 * <p>
 * Kvar event blir handtert som ein tekstbuffer for å unngå at nokon objekt tilhøyrande tidsseriens domenemodell
 * skal bevege seg over i old-generation kun som ein konsekvens av at dei er tilgjengelig via ringbufferen som
 * held på observasjonseventane. Ringbufferen forventast å relativt fort bevege seg over i old-generation som ein
 * effekt at den lever på tvers av alle obserasjonar generert frå ein og samme partisjon i gridet.
 *
 * @author Tarjei Skorgenes
 */
class ObservasjonsEvent {
    /**
     * Ein buffer som den tekstlige representasjonen av ein tidsserieobservasjon skal mellomlagrast i
     * frå observasjonen er ferdig generert til den blir lagra til disk.
     */
    final StringBuilder buffer = new StringBuilder(128);
}
