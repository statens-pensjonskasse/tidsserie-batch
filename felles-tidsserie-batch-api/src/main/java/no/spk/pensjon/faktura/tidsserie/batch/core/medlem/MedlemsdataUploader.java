package no.spk.pensjon.faktura.tidsserie.batch.core.medlem;

import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * {@link MedlemsdataUploader} representerer ein akkumulator som lar klienten styre når og kva medlemslinjer som
 * skal overførast til tidsseriebackenden.
 * <p>
 * Den antatte bruken av akkumulatoren er via {@link Stream#reduce(BinaryOperator)} for å trigge overføring av alle
 * data for eit enkeltmedlem straks straumen beveger seg frå ei linje som tilhøyrer eit medlem til ei linje som
 * tilhøyrer eit anna medlem. Dette baserer seg på ei forutsetning om at straumen er sortert pr medlem.
 * <p>
 *
 * @author Tarjei Skorgenes
 * @see MedlemsdataBackend#uploader()
 */
public interface MedlemsdataUploader {
    /**
     * Legger til <code>linje</code> i datasettet som akkumulatoren
     * vil overføre til tidsseriebackenden neste gang {@link #run()} blir kalla.
     *
     * @param linje ei linje med medlemsdata for eit medlem
     */
    void append(Medlemslinje linje);

    /**
     * Overfører alle medlemslinjer frå akkumulatoren til tidsseriebackenden.
     */
    void run();
}
