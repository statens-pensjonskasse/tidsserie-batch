package no.spk.pensjon.faktura.tidsserie.storage.disruptor;

import static java.util.Optional.of;

import java.util.Optional;

/**
 * {@link ObservasjonsEvent} inneheld den serialiserte representasjonen av ein tidsserieobservasjon p�
 * stillingsforholdniv�.
 * <br>
 * Kvar event blir handtert som ein tekstbuffer for � unng� at nokon objekt tilh�yrande tidsseriens domenemodell
 * skal bevege seg over i old-generation kun som ein konsekvens av at dei er tilgjengelig via ringbufferen som
 * held p� observasjonseventane. Ringbufferen forventast � relativt fort bevege seg over i old-generation som ein
 * effekt at den lever p� tvers av alle obserasjonar generert fr� ein og samme partisjon i gridet.
 * <br>
 * For event har tilknytta eit serienummer som kan benyttast av {@link ObservasjonsConsumer}-implementasjonar
 * til rute eventar p� ein slik m�te at eventar tilh�yrande samme serie blir samlokaliserte og liggande i samme
 * rekkef�lge som dei vart mottatt fr� klienten i. Dette er �nskelig for eksempel for � sikre at eventar tilh�yrande
 * samme stillingsforhold blir liggande i samme fil i kronologisk rekkef�lge for live-tidsserien d� det forenklar
 * feils�king radikalt.
 *
 * @author Tarjei Skorgenes
 */
public final class ObservasjonsEvent {
    /**
     * Ein buffer som den tekstlige representasjonen av ein tidsserieobservasjon skal mellomlagrast i
     * fr� observasjonen er ferdig generert til den blir lagra til disk.
     */
    public final StringBuilder buffer = new StringBuilder(512);

    private Optional<Long> serienummer = Optional.empty();

    /**
     * Identifikator for serien eventen tilh�yrer.
     *
     * @return eit tall som identifiserer serien, eller {@link Optional#empty() ingenting}
     * dersom eventen ikkje har blitt tilknytta ein serie av klienten
     */
    public Optional<Long> serienummer() {
        return serienummer;
    }

    /**
     * Overstyrer serienummeret eventen tilh�yrer.
     *
     * @param serienummer eit positivt heiltall st�rre enn 0
     * @return <code>this</code>
     * @throws IllegalArgumentException dersom serienummeret er mindre enn 1
     */
    public ObservasjonsEvent serienummer(final long serienummer) throws IllegalArgumentException {
        if (serienummer < 1) {
            throw new IllegalArgumentException(
                    "serienummer m� vere eit positivt heiltal, men var " + serienummer
            );
        }
        this.serienummer = of(serienummer);
        return this;
    }

    /**
     * Erstattar innholdet av eventen med <code>content</code>.
     *
     * @param content det nye, tekstlige innholdet til eventen
     * @return <code>this</code>
     */
    public ObservasjonsEvent medInnhold(final String content) {
        reset();
        buffer.append(content);
        return this;
    }

    /**
     * Nullstiller innholdet til eventen.
     *
     * @return event for chaining
     */
    public ObservasjonsEvent reset() {
        buffer.setLength(0);
        return this;
    }
}
