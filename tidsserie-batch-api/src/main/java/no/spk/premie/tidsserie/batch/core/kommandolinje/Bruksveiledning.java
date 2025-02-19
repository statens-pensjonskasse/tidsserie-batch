package no.spk.premie.tidsserie.batch.core.kommandolinje;

/**
 * {@link Bruksveiledning} representerer teksten som skal informere brukaren om
 * kva argument batchen støttar.
 * <p>
 * Den vil typisk bli vist når brukaren ber om hjelp, eller når brukaren har forsøkt å køyre batchen med 1 eller fleire ugyldige
 * argument.
 *
 * @see BruksveiledningSkalVisesException
 * @see UgyldigKommandolinjeArgumentException
 * @since 1.1.0
 */
public interface Bruksveiledning {
    String vis();
}