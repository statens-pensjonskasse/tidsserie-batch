package no.spk.pensjon.faktura.tidsserie.batch;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

/**
 * Datalager som styrer tilgang til grunnlagsdatane som tidligare k�yringar av faktura-grunnlagsdata-batch har produsert.
 *
 * @author Tarjei Skorgenes
 */
public interface GrunnlagsdataRepository {
    /**
     * �pnar ein straum som leser inn linje for linje fr� medlemsdatafila.
     * <br>
     * Straumen leser inn medlemsdatane just-in-time uten � realisere heile datasettet i minne p� samme tid.
     * <br>
     * Etter at straumen har blitt delvis eller fullt ut lest inn, m� den lukkast for � lukke medlemsdatafila
     * som det har blitt lest linjer fr�.
     * <br>
     * Eventuelle kommentarlinjer blir filtrert bort og vil ikkje vere ein del av den returnerte straumen.
     *
     * @return ein straum av linjer som inneheld medlemsdata
     * @throws UncheckedIOException viss ein I/O-relatert feil oppst�r
     */
    Stream<List<String>> medlemsdata();

    /**
     * �pnar ein straum som leser inn linje for linje fr� alle referansedatafiler og konverterer linjas innhold til
     * ei tidsperiode.
     * <br>
     * Straumen leser inn referansedatane just-in-time uten � realisere heile datasettet i minne p� samme tid.
     * <br>
     * Etter at straumen har blitt delvis eller fullt ut lest inn, m� den lukkast for � lukke eventuelle �pne
     * filer som det har blitt lest tidsperioder fr�.
     * <br>
     * Eventuelle kommentarlinjer blir filtrert bort og vil ikkje vere ein del av den returnerte straumen.
     *
     * @return ein straum med tidsperioder fr� alle referansedatafiler
     * @throws UncheckedIOException viss ein I/O-relatert feil oppst�r
     */
    Stream<Tidsperiode<?>> referansedata();
}
