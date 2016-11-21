package no.spk.felles.tidsserie.batch.core;

import java.util.stream.Stream;

import no.spk.felles.tidsperiode.Tidsperiode;

/**
 * {@link TidsperiodeFactory} er ei teneste som modusane kan plugge inn for å
 * effektivisere oppslag og uthenting av {@link GrunnlagsdataRepository#referansedata()}.
 * <br>
 * Framfor å måtte lese inn alle referansedata på nytt kvar gang ein treng ei eller fleire
 * av tidsperiodene i dette datasettet, kan ein via denne tenesta hente ut ein pre-innlasta
 * versjon av referansedatane av den aktuelle typen ein ønskjer å hente ut.
 * <br>
 * Det forutsetter at tenesta som implementerer dette grensesnittet også har blitt plugga
 * inn som ein {@link LastOppGrunnlagsdataKommando} slik at den kan lese inn referansedatane
 * under oppstart av batchen
 *
 * @author Tarjei Skorgenes
 */
public interface TidsperiodeFactory {
    /**
     * Hentar ut alle tidsperiodiserte lønnsdata som ikkje er medlemsspesifikke.
     *
     * @return alle lønnsdata
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode
     * @see no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperioder
     */
    Stream<Tidsperiode<?>> loennsdata();

    /**
     * Hentar ut alle tidsperioder av ei bestemt type
     *
     * @param <T> periodetypa som skal hentast ut
     * @param type periodetypa som skal hentast ut
     * @return alle tidsperioder av type <code>type</code>
     */
    <T> Stream<T> perioderAvType(Class<T> type);
}
