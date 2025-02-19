package no.spk.felles.tidsserie.batch.core.lagring;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

import java.util.Optional;

/**
 * {@link Tidsserierad} inneheld den serialiserte representasjonen av ein tidsserieobservasjon.
 * <br>
 * Kvar rad blir handtert som ein tekstbuffer for å unngå at nokon objekt tilhøyrande tidsseriens domenemodell
 * skal bevege seg over i old-generation kun som ein konsekvens av at dei er tilgjengelig via ringbufferen som
 * held på observasjonseventane. Ringbufferen forventast å relativt fort bevege seg over i old-generation som ein
 * effekt at den lever på tvers av alle obserasjonar generert frå ein og samme partisjon i gridet.
 * <br>
 * Kvar rad er tilknytta eit serienummer som kan benyttast til å rute rader på ein slik måte at alle rader
 * tilhøyrande samme serienummer blir samlokaliserte og liggande i samme fil. Dette er ønskelig for eksempel
 * for å sikre at alle rader tilhøyrande samme medlem blir liggande i samme fil.
 *
 * @author Tarjei Skorgenes
 */
public final class Tidsserierad {
    /**
     * Ein buffer som den tekstlige representasjonen av ein tidsserieobservasjon skal mellomlagrast i
     * frå observasjonen er ferdig generert til den blir lagra til disk.
     */
    public final StringBuilder buffer = new StringBuilder(512);

    private Optional<Long> serienummer = Optional.empty();

    public String filprefix = "tidsserie";

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
    public Tidsserierad serienummer(final long serienummer) throws IllegalArgumentException {
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
    public Tidsserierad medInnhold(final String content) {
        reset();
        buffer.append(content);
        return this;
    }

    /**
     * Nullstiller innholdet til eventen.
     *
     * @return event for chaining
     */
    public Tidsserierad reset() {
        buffer.setLength(0);
        return this;
    }

    public Tidsserierad medFilprefix(final String filprefix) throws IllegalArgumentException{
        if (requireNonNull(filprefix, "filprefix er påkrevd, men var null").trim().length() == 0) {
            throw new IllegalArgumentException("Filprefix må ha en verdi men var tom.");
        }
        this.filprefix = filprefix;
        return this;
    }
}
