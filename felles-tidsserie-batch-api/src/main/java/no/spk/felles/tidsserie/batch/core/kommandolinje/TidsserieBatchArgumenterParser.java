package no.spk.felles.tidsserie.batch.core.kommandolinje;

/**
 * @since 1.1.0
 */
public interface TidsserieBatchArgumenterParser {
    TidsserieBatchArgumenter parse(String... args)
            throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException;
}