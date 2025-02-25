package no.spk.tidsserie.batch.core;

import java.util.stream.Stream;

/**
 * Blir kasta dersom ei eller fleire {@link TidsserieLivssyklus}-tjeneste feilar ved start eller stop.
 *
 * @author Tarjei Skorgenes
 */
public class TidsserieLivssyklusException extends RuntimeException {
    private final static long serialVersionUID = 1;

    private final String operasjon;

    /**
     * Konstruerer ein ny feil som indikerer at ein uforventa feil oppstod som følge av den angitte operasjonen på ein eller fleire livssyklusar.
     * <br>
     * Merk at alle feila frå {@code errors} vil bli lagt til som {@link Exception#getSuppressed() suppressed}-exceptions.
     *
     * @param operasjon livssyklusoperasjonen som feila
     * @param errors    feila som oppstod
     */
    public TidsserieLivssyklusException(final String operasjon, final Stream<RuntimeException> errors) {
        this.operasjon = operasjon;
        errors.forEach(this::addSuppressed);
    }

    @Override
    public String getMessage() {
        return "Ein uventa feil oppstod i " + operasjon + "-operasjonen av tidsserielivssyklusen";
    }
}
