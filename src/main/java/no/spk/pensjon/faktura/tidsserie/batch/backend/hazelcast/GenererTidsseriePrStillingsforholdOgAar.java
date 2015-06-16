package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.stream.Stream.concat;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aarsverk;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.PrognoseRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.disruptor.LmaxDisruptorPublisher;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.mapreduce.Context;
import com.hazelcast.mapreduce.LifecycleMapperAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GenererTidsseriePrStillingsforholdOgAar
        extends LifecycleMapperAdapter<String, List<List<String>>, String, Integer> implements HazelcastInstanceAware {
    private final LocalDate foerstedato;
    private final LocalDate sistedato;

    private transient LmaxDisruptorPublisher publisher;
    private transient TidsserieFactory grunnlagsdata;
    private transient NumberFormat format;

    GenererTidsseriePrStillingsforholdOgAar(final LocalDate foerstedato, final LocalDate sistedato) {
        this.foerstedato = foerstedato;
        this.sistedato = sistedato;
    }

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcast) {
        this.grunnlagsdata = (TidsserieFactory) hazelcast.getUserContext().get(TidsserieFactory.class.getSimpleName());
        this.publisher = (LmaxDisruptorPublisher) hazelcast.getUserContext().get(LmaxDisruptorPublisher.class.getSimpleName());
        if (format == null) {
            format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(3);
            format.setMinimumFractionDigits(1);
        }
    }

    public void publishHeader(final LmaxDisruptorPublisher lager) {
        lager.publiser(builder -> {
            builder.append("avtaleId;stillingsforholdId;observasjonsdato;maskinelt_grunnlag;premiestatus;årsverk;personnummer\n");
        });
    }

    @Override
    public void map(final String key, final List<List<String>> value, final Context<String, Integer> context) {
        final Logger log = LoggerFactory.getLogger(getClass());

        final TidsserieFacade tidsserie = grunnlagsdata.create(
                lagFeilhandteringForMedlem(key, context, log)
        );
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
            tidsserie.generer(
                    grunnlagsdata.create(key, value),
                    new Observasjonsperiode(foerstedato, sistedato),
                    tidsserie.lagObservasjonsaggregatorPrStillingsforholdOgAvtale(publikator),
                    concat(
                            new PrognoseRegelsett().reglar(),
                            grunnlagsdata.loennsdata()
                    )
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