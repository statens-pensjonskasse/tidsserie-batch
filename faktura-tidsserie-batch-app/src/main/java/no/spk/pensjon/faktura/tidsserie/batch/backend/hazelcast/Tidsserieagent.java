package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.mapreduce.Context;
import com.hazelcast.mapreduce.LifecycleMapperAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * {@link Tidsserieagent} er limet som binder saman data lasta opp til in-memory gridet i Hazelcast og
 * domenemodellen som bygger opp tidsseriar.
 *
 * @author Tarjei Skorgenes
 */
class Tidsserieagent
        extends LifecycleMapperAdapter<String, List<List<String>>, String, Integer> implements HazelcastInstanceAware {
    public static final String MDC_SERIENUMMER = "serienummer";
    private final LocalDate foerstedato;
    private final LocalDate sistedato;

    private transient GenererTidsserieCommand kommando;
    private transient IAtomicLong serienummerGenerator;
    private transient long serienummer;

    Tidsserieagent(final LocalDate foerstedato, final LocalDate sistedato) {
        this.foerstedato = foerstedato;
        this.sistedato = sistedato;
    }

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcast) {
        configure(hazelcast.getUserContext());
        serienummerGenerator = hazelcast.getAtomicLong("serienummer");
    }

    void configure(final Map<String, Object> userContext) {
        final TidsserieFactory grunnlagsdata = lookup(userContext, TidsserieFactory.class);
        final StorageBackend publisher = lookup(userContext, StorageBackend.class);
        final Tidsseriemodus parameter = lookup(userContext, Tidsseriemodus.class);
        this.kommando = new GenererTidsserieCommand(grunnlagsdata, publisher, parameter);
    }

    @Override
    public void initialize(final Context<String, Integer> context) {
        // Serienummeret for alle eventar som blir generert for medlemmar i gjeldande partisjon
        serienummer = serienummerGenerator.getAndIncrement();
        MDC.put(MDC_SERIENUMMER, "" + serienummer);
    }

    @Override
    public void finalized(final Context<String, Integer> context) {
        MDC.remove(MDC_SERIENUMMER);
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
                    feilhandtering,
                    serienummer
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

    static <T> T lookup(final Map<String, Object> userContext, final Class<T> serviceType) {
        return ofNullable(
                userContext.get(serviceType.getSimpleName())
        )
                .filter(service -> serviceType.isAssignableFrom(service.getClass()))
                .map(serviceType::cast)
                .orElseThrow(() -> new IllegalStateException(
                                "Ingen teneste av type "
                                        + serviceType.getSimpleName()
                                        + " er registrert i tenesteregisteret.\n"
                                        + "Tilgjengelig tenester:\n"
                                        + userContext
                                        .values()
                                        .stream()
                                        .map(Object::getClass)
                                        .map(Class::getSimpleName)
                                        .map(type -> "- " + type)
                                        .collect(joining("\n"))
                        )
                )
                ;
    }
}
