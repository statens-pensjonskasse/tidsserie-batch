package no.spk.felles.tidsserie.batch.main;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.LastOppGrunnlagsdataKommando;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.felles.tidsserie.batch.core.ServiceLocator;
import no.spk.felles.tidsserie.batch.core.TidsperiodeFactory;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperioder;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;
import no.spk.felles.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.StandardAvtaleInformasjonRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.felles.tidsserie.batch.core.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.AvtalekoblingOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.MedregningsOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.StillingsendringOversetter;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link GrunnlagsdataService} opptrer som bindeledd mellom
 * beregningsbackenden og grunnlagsdatane frå flate filer på disk.
 * <br>
 * Via denne tenesta kan CSV-formaterte grunnlagsdata generert av faktura-grunnlagsdata-batch bli lest inn frå disk
 * og lastast opp til beregningsbackenden. I tillegg kan beregningsbackenden hente ut avtale- og medlemsuavhengige
 * lønnsdata ved oppstart av tidsseriegenereringa.
 *
 * @author Tarjei Skorgenes
 */
public class GrunnlagsdataService implements TidsserieFactory, LastOppGrunnlagsdataKommando {
    private final Map<Class<?>, List<Tidsperiode<?>>> perioder = new HashMap<>();

    private AvtaleinformasjonRepository avtaleinformasjonRepository = (a) -> Stream.empty();

    private final Map<Class<?>, MedlemsdataOversetter<?>> medlemsdataOversetter = new HashMap<>();

    /**
     * Konstruerer ei ny teneste som lastar inn og koordinerer opplasting og tilgang til medlems- og referansedata.
     */
    public GrunnlagsdataService() {
        medlemsdataOversetter.put(Stillingsendring.class, new StillingsendringOversetter());
        medlemsdataOversetter.put(Avtalekoblingsperiode.class, new AvtalekoblingOversetter());
        medlemsdataOversetter.put(Medregningsperiode.class, new MedregningsOversetter());
    }

    @Override
    public TidsserieFacade create(final Feilhandtering feilhandtering) {
        final TidsserieFacade fasade = new TidsserieFacade();
        fasade.overstyr(avtaleinformasjonRepository);
        fasade.overstyr(requireNonNull(feilhandtering, "feilhandteringsstrategi er påkrevd, men var null"));
        return fasade;
    }

    @Override
    public Medlemsdata create(final List<List<String>> data) {
        return new Medlemsdata(
                requireNonNull(data, "medlemsdata er påkrevd, men var null"),
                medlemsdataOversettere()
        );
    }

    @Override
    public Stream<Tidsperiode<?>> loennsdata() {
        return Stream.concat(
                perioderAvType(Loennstrinnperioder.class),
                perioderAvType(Omregningsperiode.class)
        );
    }

    @Override
    public <T> Stream<T> perioderAvType(final Class<T> type) {
        return perioder.getOrDefault(type, emptyList()).stream().map(type::cast);
    }

    /**
     * Leser inn alle medlemsdata og lastar dei opp til medlemsdatabackenden.
     * <br>
     * I tillegg blir alle referansedata lest inn og eagerly konvertert til tidsperioder for rask access via
     * {@link TidsperiodeFactory}.
     *
     * @param registry {@inheritDoc}
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

        lesInnReferansedata(repository);
    }

    void lesInnReferansedata(final GrunnlagsdataRepository repository) {
        try (final Stream<Tidsperiode<?>> referansedata = repository.referansedata()) {
            perioder.putAll(referansedata.collect(groupingBy(Object::getClass)));
        }
        perioder.put(Loennstrinnperioder.class, grupperLoennstrinnperioder());
        avtaleinformasjonRepository = new StandardAvtaleInformasjonRepository(perioder);
    }

    Map<Class<?>, MedlemsdataOversetter<?>> medlemsdataOversettere() {
        return medlemsdataOversetter;
    }

    private List<Tidsperiode<?>> grupperLoennstrinnperioder() {
        return Stream.of(
                Loennstrinnperioder.grupper(
                        Ordning.POA,
                        apotekloennstrinn()
                ),
                Loennstrinnperioder.grupper(
                        Ordning.SPK,
                        statligeloennstrinn()
                )
        )
                .flatMap(s -> s)
                .collect(toList());
    }

    private Stream<Loennstrinnperiode<?>> statligeloennstrinn() {
        return perioderAvType(StatligLoennstrinnperiode.class)
                .map(StatligLoennstrinnperiode.class::cast);
    }

    private Stream<Loennstrinnperiode<?>> apotekloennstrinn() {
        return perioderAvType(ApotekLoennstrinnperiode.class)
                .map(ApotekLoennstrinnperiode.class::cast);
    }
}
