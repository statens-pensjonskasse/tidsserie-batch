package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

/**
 * {@link ObservasjonsEvent} inneheld den serialiserte representasjonen av ein tidsserieobservasjon p�
 * stillingsforholdniv�.
 * <p>
 * Kvar event blir handtert som ein tekstbuffer for � unng� at nokon objekt tilh�yrande tidsseriens domenemodell
 * skal bevege seg over i old-generation kun som ein konsekvens av at dei er tilgjengelig via ringbufferen som
 * held p� observasjonseventane. Ringbufferen forventast � relativt fort bevege seg over i old-generation som ein
 * effekt at den lever p� tvers av alle obserasjonar generert fr� ein og samme partisjon i gridet.
 *
 * @author Tarjei Skorgenes
 */
class ObservasjonsEvent {
    /**
     * Ein buffer som den tekstlige representasjonen av ein tidsserieobservasjon skal mellomlagrast i
     * fr� observasjonen er ferdig generert til den blir lagra til disk.
     */
    final StringBuilder buffer = new StringBuilder(128);
}
