package no.spk.pensjon.faktura.tidsserie.batch.core;

import static java.util.Optional.of;

import java.util.Optional;

/**
 * {@link ObservasjonsEvent} inneheld den serialiserte representasjonen av ein tidsserieobservasjon på
 * stillingsforholdnivå.
 * <br>
 * Kvar event blir handtert som ein tekstbuffer for å unngå at nokon objekt tilhøyrande tidsseriens domenemodell
 * skal bevege seg over i old-generation kun som ein konsekvens av at dei er tilgjengelig via ringbufferen som
 * held på observasjonseventane. Ringbufferen forventast å relativt fort bevege seg over i old-generation som ein
 * effekt at den lever på tvers av alle obserasjonar generert frå ein og samme partisjon i gridet.
 *
 * @author Tarjei Skorgenes
 */
public final class ObservasjonsEvent {
    /**
     * Ein buffer som den tekstlige representasjonen av ein tidsserieobservasjon skal mellomlagrast i
     * frå observasjonen er ferdig generert til den blir lagra til disk.
     */
    public final StringBuilder buffer = new StringBuilder(512);

    private Optional<Long> serienummer = Optional.empty();

    /**
     * Identifikator for serien eventen tilhøyrer.
     *
     * @return eit tall som identifiserer serien, eller {@link Optional#empty() ingenting}
     * dersom eventen ikkje har blitt tilknytta ein serie av klienten
     */
    public Optional<Long> serienummer() {
        return serienummer;
    }

    /**
     * Overstyrer serienummeret eventen tilhøyrer.
     *
     * @param serienummer eit positivt heiltall større enn 0
     * @return <code>this</code>
     * @throws IllegalArgumentException dersom serienummeret er mindre enn 1
     */
    public ObservasjonsEvent serienummer(final long serienummer) throws IllegalArgumentException {
        if (serienummer < 1) {
            throw new IllegalArgumentException(
                    "serienummer må vere eit positivt heiltal, men var " + serienummer
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
