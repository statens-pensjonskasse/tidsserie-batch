package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
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

    private transient GenererTidsserieCommand kommando;

    GenererTidsseriePrStillingsforholdOgAar(final LocalDate foerstedato, final LocalDate sistedato) {
        this.foerstedato = foerstedato;
        this.sistedato = sistedato;
    }

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcast) {
        configure(hazelcast.getUserContext());
    }

    void configure(final Map<String, Object> userContext) {
        final TidsserieFactory grunnlagsdata = lookup(userContext, TidsserieFactory.class);
        final StorageBackend publisher = lookup(userContext, StorageBackend.class);
        final Tidsseriemodus parameter = lookup(userContext, Tidsseriemodus.class);
        this.kommando = new GenererTidsserieCommand(grunnlagsdata, publisher, parameter);
    }

    @Override
    public void map(final String key, final List<List<String>> value, final Context<String, Integer> context) {
        final Logger log = LoggerFactory.getLogger(getClass());

        final Feilhandtering feilhandtering = lagFeilhandteringForMedlem(key, context, log);
        context.emit("medlem", 1);

        try {
            kommando.generer(
                    value,
                    new Observasjonsperiode(foerstedato, sistedato),
                    feilhandtering
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

    private <T> T lookup(final Map<String, Object> userContext, final Class<T> serviceType) {
        return serviceType.cast(
                userContext.get(serviceType.getSimpleName())
        );
    }
}