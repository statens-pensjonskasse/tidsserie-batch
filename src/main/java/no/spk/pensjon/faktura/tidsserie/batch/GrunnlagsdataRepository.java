package no.spk.pensjon.faktura.tidsserie.batch;

import java.io.IOException;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

/**
 * Datalager som styrer tilgang til grunnlagsdatane som tidligare køyringar av faktura-grunnlagsdata-batch har produsert.
 *
 * @author Tarjei Skorgenes
 */
public interface GrunnlagsdataRepository {
    Stream<String> medlemsdata() throws IOException;

    Stream<Tidsperiode<?>> referansedata() throws IOException;
}
