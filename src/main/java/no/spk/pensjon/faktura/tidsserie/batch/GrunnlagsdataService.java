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
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleinformasjonRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.storage.csv.AvtalekoblingOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.MedregningsOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.StillingsendringOversetter;

/**
 * {@link no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService} opptrer som bindeledd mellom
 * beregningsbackenden og grunnlagsdatane fr� flate filer p� disk.
 * <br>
 * Via denne tenesta kan CSV-formaterte grunnlagsdata generert av faktura-grunnlagsdata-batch bli lest inn fr� disk
 * og lastast opp til beregningsbackenden. I tillegg kan beregningsbackenden hente ut avtale- og medlemsuavhengige
 * l�nnsdata ved oppstart av tidsseriegenereringa.
 *
 * @author Tarjei Skorgenes
 */
public class GrunnlagsdataService implements TidsserieFactory {
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
        this.backend = requireNonNull(backend, "backend er p�krevd, men var null");
        this.input = requireNonNull(repository, "inputfiler er p�krevd, men var null");
    }

    @Override
    public TidsserieFacade create(final Feilhandtering feilhandtering) {
        final TidsserieFacade fasade = new TidsserieFacade();
        fasade.overstyr(this::finn);
        fasade.overstyr(requireNonNull(feilhandtering, "feilhandteringsstrategi er p�krevd, men var null"));
        return fasade;
    }

    @Override
    public Medlemsdata create(final String foedselsnummer, final List<List<String>> data) {
        return new Medlemsdata(
                requireNonNull(data, "medlemsdata er p�krevd, men var null"),
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

    /**
     * Leser inn alle medlems-, avtale- og l�nnsdata fr� inputfilene og overf�rer dei til backenden.
     *
     * @throws UncheckedIOException dersom innlesinga av grunnlagsdata feilar p� grunn av I/O-relaterte issues
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

        backend.registrer(TidsserieFactory.class, this);
    }

    void lesInnReferansedata() {
        try (final Stream<Tidsperiode<?>> referansedata = input.referansedata()) {
            perioder.putAll(referansedata.collect(groupingBy(Object::getClass)));
        }
        perioder.put(Loennstrinnperioder.class, grupperLoennstrinnperioder());
        avtalar.putAll(grupperAvtaleperioder());
    }

    Map<Class<?>, MedlemsdataOversetter<?>> medlemsdataOversettere() {
        final Map<Class<?>, MedlemsdataOversetter<?>> oversettere = new HashMap<>();
        oversettere.put(Stillingsendring.class, new StillingsendringOversetter());
        oversettere.put(Avtalekoblingsperiode.class, new AvtalekoblingOversetter());
        oversettere.put(Medregningsperiode.class, new MedregningsOversetter());
        return oversettere;
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
    private Stream<Tidsperiode<?>> finn(final AvtaleId avtale) {
        return avtalar.getOrDefault(avtale, emptyList()).stream().map((Tidsperiode<?> p) -> p);
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
