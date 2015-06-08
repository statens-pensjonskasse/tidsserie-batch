package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.ReferansedataService;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserieobservasjonsgenerator;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aarsverk;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Avtalekoblingsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Stillingsendring;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.csv.AvtalekoblingOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.MedregningsOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.StillingsendringOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.disruptor.LmaxDisruptorPublisher;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.mapreduce.Context;
import com.hazelcast.mapreduce.LifecycleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GenererTidsseriePrStillingsforholdOgAar implements LifecycleMapper<String, List<List<String>>, String, Integer>, HazelcastInstanceAware {
    private final File file;
    private final LocalDate foerstedato;
    private final LocalDate sistedato;

    private transient LmaxDisruptorPublisher publisher;
    private transient ExecutorService executor;
    private transient Tidsserieobservasjonsgenerator generator;
    private transient NumberFormat format;

    GenererTidsseriePrStillingsforholdOgAar(final File destination, final LocalDate foerstedato, LocalDate sistedato) {
        this.file = destination;
        this.foerstedato = foerstedato;
        this.sistedato = sistedato;
    }

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcast) {
        final ReferansedataService grunnlagsdata = (ReferansedataService) hazelcast.getUserContext().get(ReferansedataService.class.getSimpleName());
        if (generator == null) {
            // TODO: Horribel sjite i den her, alle eksterne datasett og tjenester bør trulig gjerast tilgjengelig
            // via user contexten til hazelcastinstansen.
            generator = new Tidsserieobservasjonsgenerator();
            generator.registrer(grunnlagsdata.loennsdata());
            generator.overstyr(grunnlagsdata::finn);

            format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(3);
            format.setMinimumFractionDigits(1);
        }
        executor = (ExecutorService) hazelcast
                .getUserContext()
                .computeIfAbsent(
                        Executor.class.getSimpleName(),
                        c -> Executors.newCachedThreadPool(
                                r -> new Thread(r, "lmax-disruptor-" + System.currentTimeMillis())
                        )
                );
    }

    @Override
    public void initialize(final Context<String, Integer> context) {
        // TODO: Uøndvendig gitt at ein kan sende inn ein lmax disruptor pr hazelcast-node/instans via user contexten
        // Alternativt bruk kun ein disruptor pr JVM og la den sjølv skrive til ei stor fil, eller splitte i mindre
        // filer basert på maksimalt ønska antall medlemmar pr fil or something like that
        publisher = new LmaxDisruptorPublisher(executor, file);
        publisher.start();
        publisher.publiser(builder -> {
            builder.append("avtaleId;stillingsforholdId;observasjonsdato;maskinelt_grunnlag;premiestatus;årsverk;personnummer\n");
        });
    }

    @Override
    public void finalized(final Context<String, Integer> context) {
        publisher.stop();
    }

    @Override
    public void map(final String key, final List<List<String>> value, final Context<String, Integer> context) {
        final Observasjonsperiode observasjonsperiode = new Observasjonsperiode(foerstedato, sistedato);

        final Logger log = LoggerFactory.getLogger(getClass());
        final Map<Class<?>, MedlemsdataOversetter<?>> oversettere = new HashMap<>();
        oversettere.put(Stillingsendring.class, new StillingsendringOversetter());
        oversettere.put(Avtalekoblingsperiode.class, new AvtalekoblingOversetter());
        oversettere.put(Medregningsperiode.class, new MedregningsOversetter());

        final Medlemsdata medlem = new Medlemsdata(value, oversettere);
        context.emit("medlem", 1);
        try {
            final Consumer<TidsserieObservasjon> publikator = o -> {
                context.emit("observasjon", 1);
                publisher.publiser(builder -> {
                    builder
                            .append(o.avtale().id())
                            .append(';')
                            .append(o.stillingsforhold.id())
                            .append(';')
                            .append(o.observasjonsdato.dato())
                            .append(';')
                            .append(o.maskineltGrunnlag.verdi())
                            .append(';')
                            .append(o.premiestatus().map(Premiestatus::kode).orElse("UKJENT"))
                            .append(';')
                            .append(o.maaling(Aarsverk.class)
                                            .map(Aarsverk::tilProsent)
                                            .map(Prosent::toDouble)
                                            .map(format::format)
                                            .orElse("0.0")
                            )
                            .append(';')
                            .append(key)
                            .append('\n');
                });
            };
            generator.observerForVekstmodell(
                    medlem,
                    observasjonsperiode,
                    generator.lagObservasjonsaggregatorPrStillingsforholdOgAvtale(publikator),
                    lagFeilhandteringForMedlem(key, context, log)
            );
        } catch (final RuntimeException | Error e) {
            log.warn("Periodisering av medlem {} feila: {} (endringar = {})", key, e.getMessage(), value);
            log.info("Feilkilde:", e);
            emitError(context, e);
        }
    }

    private Feilhandtering lagFeilhandteringForMedlem(final String medlem, final Context<String, Integer> context,
                                                      final Logger log) {
        return (s, u, t) -> {
            log.warn("Observering av stillingsforhold feila: {} (medlem = {}, stillingsforhold = {})", t.getMessage(), medlem, s.id());
            log.info("Feilkilde:", t);
            log.debug("Underlag: {}", u);
            emitError(context, t);
        };
    }

    private void emitError(Context<String, Integer> context, Throwable t) {
        context.emit("errors", 1);
        context.emit("errors_type_" + t.getClass().getSimpleName(), 1);
        context.emit("errors_message_" + (t.getMessage() != null ? t.getMessage() : "null"), 1);
    }
}