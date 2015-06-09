package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * {@link MedlemsdataUploader} representerer ein akkumulator som lar klienten styre n�r og kva medlemslinjer som
 * skal overf�rast til tidsseriebackenden.
 * <p>
 * Den antatte bruken av akkumulatoren er via {@link Stream#reduce(BinaryOperator)} for � trigge overf�ring av alle
 * data for eit enkeltmedlem straks straumen beveger seg fr� ei linje som tilh�yrer eit medlem til ei linje som
 * tilh�yrer eit anna medlem. Dette baserer seg p� ei forutsetning om at straumen er sortert pr medlem.
 * <p>
 *
 * @author Tarjei Skorgenes
 * @see TidsserieBackendService#uploader()
 */
public interface MedlemsdataUploader {
    /**
     * Legger til <code>linje</code> i datasettet som akkumulatoren
     * vil overf�re til tidsseriebackenden neste gang {@link #run()} blir kalla.
     *
     * @param linje ei linje med medlemsdata for eit medlem
     */
    void append(Medlemslinje linje);

    /**
     * Overf�rer alle medlemslinjer fr� akkumulatoren til tidsseriebackenden.
     */
    void run();

    /**
     * Lastar opp / registrerer referansedatane slik at alle data p�krevd
     * for � bygge ein tidsserie er tilgjengelige i alle delar av backenden.
     *
     * @param service tjenesta som gir backenden tilgang til referansedatane
     */
    void registrer(ReferansedataService service);
}
