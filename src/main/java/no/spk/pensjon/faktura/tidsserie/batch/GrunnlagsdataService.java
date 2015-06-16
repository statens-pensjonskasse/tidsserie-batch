package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtalerelatertperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.ApotekLoennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Loennstrinnperioder;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.StatligLoennstrinnperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository;

/**
 * Grunnlagsdata
 *
 * @author Tarjei Skorgenes
 */
public class GrunnlagsdataService implements ReferansedataService {
    private final Map<Class<?>, List<Tidsperiode<?>>> perioder = new HashMap<>();

    private final Map<AvtaleId, List<Avtalerelatertperiode<?>>> avtalar = new HashMap<>();

    private final TidsserieBackendService backend;

    private final GrunnlagsdataRepository input;

    /**
     * Konstruerer ei ny teneste som hentar grunnlagsdata via <code>repository</code> og gjer dei tilgjengelig via
     * <code>backend</code>.
     *
     * @param backend    backenden som alle grunnlagsdatane blir lasta opp til eller gjort tilgjengelig via
     * @param repository datalageret som gir oss tilgang til grunnlagsdatane generert av faktura-grunnlagsdata-batch
     * @throws NullPointerException viss nokon argument er <code>null</code>
     */
    public GrunnlagsdataService(final TidsserieBackendService backend, final GrunnlagsdataRepository repository) {
        this.backend = requireNonNull(backend, "backend er påkrevd, men var null");
        this.input = requireNonNull(repository, "inputfiler er påkrevd, men var null");
    }

    /**
     * Hentar ut alle tidsperiodiserte lønnsdata som ikkje er medlemsspesifikke.
     *
     * @return alle lønnsdata
     * @see Omregningsperiode
     * @see Loennstrinnperioder
     */
    @Override
    public Stream<Tidsperiode<?>> loennsdata() {
        return Stream.concat(
                perioderAvType(Loennstrinnperioder.class),
                perioderAvType(Omregningsperiode.class)
        );
    }

    /**
     * Hent ut alle avtaledata for avtalen.
     *
     * @param avtale avtalen som avtaledata skal hentast ut for
     * @return alle tidsperiodiserte avtaledata som er tilknytta avtalen i grunnlagsdatane
     * @see AvtaleinformasjonRepository
     * @see Avtaleversjon
     * @see Avtaleprodukt
     */
    @Override
    public Stream<Tidsperiode<?>> finn(final AvtaleId avtale) {
        return avtalar.getOrDefault(avtale, emptyList()).stream().map((Tidsperiode<?> p) -> p);
    }

    /**
     * Leser inn alle medlems-, avtale- og lønnsdata frå inputfilene og overfører dei til backenden.
     *
     * @throws UncheckedIOException dersom innlesinga av grunnlagsdata feilar på grunn av I/O-relaterte issues
     */
    public void lastOpp() {
        final MedlemsdataUploader upload = backend.uploader();
        try (final Stream<List<String>> lines = input.medlemsdata()) {
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

        lesInnReferansedata();

        upload.registrer(this);
    }

    void lesInnReferansedata() {
        try (final Stream<Tidsperiode<?>> referansedata = input.referansedata()) {
            perioder.putAll(referansedata.collect(groupingBy(Object::getClass)));
        }
        perioder.put(Loennstrinnperioder.class, grupperLoennstrinnperioder());
        avtalar.putAll(grupperAvtaleperioder());
    }


    private Map<AvtaleId, List<Avtalerelatertperiode<?>>> grupperAvtaleperioder() {
        final Stream<Avtaleversjon> versjoner = perioderAvType(Avtaleversjon.class);
        final Stream<Avtaleprodukt> produkter = perioderAvType(Avtaleprodukt.class);
        return Stream.concat(versjoner, produkter)
                .map((Avtalerelatertperiode<?> p) -> p)
                .collect(
                        groupingBy(Avtalerelatertperiode::avtale)
                );
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

    <T> Stream<T> perioderAvType(final Class<T> type) {
        return perioder.getOrDefault(type, emptyList()).stream().map(type::cast);
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
