package no.spk.felles.tidsserie.batch.core.medlem;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.GrunnlagsdataRepository;
import no.spk.felles.tidsserie.batch.core.LastOppGrunnlagsdataKommando;
import no.spk.felles.tidsserie.batch.core.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link MedlemsdataOpplaster} koordinerer innlesing og opplasting av data frå
 * {@link GrunnlagsdataRepository#medlemsdata()} til {@link MedlemsdataBackend}.
 * <br>
 * Via denne tenesta kan medlemsdata generert av ei eller anna form for
 * medlemsdata-uttrekk bli lest inn og lasta opp til medlemsdatabackenden under oppstart
 * av batchen.
 *
 * @author Tarjei Skorgenes
 */
public class MedlemsdataOpplaster implements LastOppGrunnlagsdataKommando {

    /**
     * Leser inn alle medlemsdata og lastar dei opp til medlemsdatabackenden.
     *
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void lastOpp(final ServiceRegistry registry) {
        final ServiceLocator services = new ServiceLocator(registry);

        final MedlemsdataUploader upload = services
                .firstMandatory(MedlemsdataBackend.class)
                .uploader();
        final GrunnlagsdataRepository repository = services.firstMandatory(GrunnlagsdataRepository.class);
        try (final Stream<List<String>> lines = repository.medlemsdata()) {
            lines
                    .map(Medlemslinje::new)
                    .reduce((first, second) -> {
                        upload.append(first);

                        if (!first.tilhoeyrer(second.medlem())) {
                            upload.run();
                        }
                        return second;
                    })
                    .ifPresent(cells -> {
                        upload.append(cells);
                        upload.run();
                    });
        }
    }
}
