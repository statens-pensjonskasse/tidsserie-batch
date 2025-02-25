package no.spk.tidsserie.batch.core.grunnlagsdata;

import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;

/**
 * {@link UgyldigUttrekkException} blir kasta dersom
 * {@link TidsserieBatchArgumenter#uttrekkskatalog() uttrekket} batchen skal lese inn grunnlagsdata frå,
 * blir flagga som ugyldig av av {@link UttrekksValidator}
 *
 * @author Snorre E. Brekke - Computas
 * @since 1.1.0
 */
public class UgyldigUttrekkException extends RuntimeException {
    private final static long serialVersionUID = 1;

    public UgyldigUttrekkException(String message) {
        super(message);
    }

    public UgyldigUttrekkException(Throwable cause) {
        super(cause);
    }
}
