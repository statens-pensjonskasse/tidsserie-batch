package no.spk.felles.tidsserie.batch.core;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import no.spk.felles.tidsperiode.Tidsperiode;

/**
 * Datalager som styrer tilgang til grunnlagsdatane som batchen skal lese inn og tilby for bruk til {@link Tidsseriemodus}
 * ved generering av tidsseriar.
 *
 * @author Tarjei Skorgenes
 */
public interface GrunnlagsdataRepository {
    /**
     * Åpnar ein straum som leser inn linje for linje frå medlemsdatafila.
     * <br>
     * Straumen leser inn medlemsdatane just-in-time uten å realisere heile datasettet i minne på samme tid.
     * <br>
     * Etter at straumen har blitt delvis eller fullt ut lest inn, må den lukkast for å lukke medlemsdatafila
     * som det har blitt lest linjer frå.
     * <br>
     * Eventuelle kommentarlinjer blir filtrert bort og vil ikkje vere ein del av den returnerte straumen.
     *
     * @return ein straum av linjer som inneheld medlemsdata
     * @throws UncheckedIOException viss ein I/O-relatert feil oppstår
     */
    Stream<List<String>> medlemsdata();

    /**
     * Åpnar ein straum som leser inn linje for linje frå alle referansedatafiler og konverterer linjas innhold til
     * ei tidsperiode.
     * <br>
     * Straumen leser inn referansedatane just-in-time uten å realisere heile datasettet i minne på samme tid.
     * <br>
     * Etter at straumen har blitt delvis eller fullt ut lest inn, må den lukkast for å lukke eventuelle åpne
     * filer som det har blitt lest tidsperioder frå.
     * <br>
     * Eventuelle kommentarlinjer blir filtrert bort og vil ikkje vere ein del av den returnerte straumen.
     *
     * @return ein straum med tidsperioder frå alle referansedatafiler
     * @throws UncheckedIOException viss ein I/O-relatert feil oppstår
     */
    Stream<Tidsperiode<?>> referansedata();
}
