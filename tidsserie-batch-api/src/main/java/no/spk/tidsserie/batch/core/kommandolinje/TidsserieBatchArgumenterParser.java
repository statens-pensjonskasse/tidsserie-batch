package no.spk.tidsserie.batch.core.kommandolinje;

/**
 * {@link TidsserieBatchArgumenterParser} er ansvarlig for innlesing, validering og konvertering av
 * alle kommandolinjeargument til ei {@link TidsserieBatchArgumenter form} som applikasjonen
 * kan benytte seg av.
 * <p>
 * Dersom brukaren er usikker på kva argument applikasjonen tilbyr, må parsinga tilby ei mekanisme
 * for å la brukaren angi dette, typisk i form av eit form for "usage / hjelp / help"-argument.
 * Viss brukaren ber om hjelp må parsinga kaste ein {@link BruksveiledningSkalVisesException}.
 * <p>
 * Dersom brukaren ikkje har bedt om hjelp, men har angitt 1 eller fleire ukjente argument, eller
 * kjente argument med ugyldige verdiar, må parsinga kaste ein {@link UgyldigKommandolinjeArgumentException},
 * som inneheld informasjon om kva som var feil + bruksveiledning om kva batchen støtter/krever
 * av argument.
 *
 * @see TidsserieBatchArgumenter
 * @since 1.1.0
 */
public interface TidsserieBatchArgumenterParser {
    TidsserieBatchArgumenter parse(String... args)
            throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException;
}