package no.spk.felles.tidsserie.batch.main;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Blir kastet dersom en eller flere av {@link no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback}-tjenester
 * feiler ved {@link no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback#tidsserieGenerert(ServiceRegistry)}.
 *
 * @author Snorre E. Brekke - Computas
 */
public class TidsserieGenerertException extends RuntimeException {
    private final static long serialVersionUID = 1;

    /**
     * Konstruerer en ny feil som indikerer at en eller felere uventede feil oppstod når en
     * {@link no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback} ble behandlet.
     * <br>
     * Merk at alle feil fra {@code errors} vil bli lagt til som {@link Exception#getSuppressed() suppressed}-exceptions.
     *
     * @param errors feilene som oppstod
     */
    public TidsserieGenerertException(final Stream<RuntimeException> errors) {
        errors.forEach(this::addSuppressed);
    }

    @Override
    public String getMessage() {
        return "En uventet feil oppstod i etterkant av at tidsserien var ferdig generert.";
    }
}
