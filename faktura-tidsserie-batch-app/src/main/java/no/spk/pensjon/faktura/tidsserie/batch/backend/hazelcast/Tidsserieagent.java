package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.batch.core.Extensionpoint;
import no.spk.pensjon.faktura.tidsserie.batch.core.GenererTidsserieCommand;
import no.spk.pensjon.faktura.tidsserie.batch.core.ServiceLocator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

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
    private final static long serialVersionUID = 1;

    public static final String MDC_SERIENUMMER = "serienummer";

    private transient GenererTidsserieCommand kommando;
    private transient Extensionpoint<AgentInitializer> listeners;
    private transient Observasjonsperiode periode;

    private transient IAtomicLong serienummerGenerator;
    private transient long serienummer;

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcast) {
        configure(hazelcast.getUserContext());
        serienummerGenerator = hazelcast.getAtomicLong("serienummer");
    }

    void configure(final Map<String, Object> userContext) {
        final ServiceRegistry registry = lookup(userContext, ServiceRegistry.class);
        configure(registry);
    }

    void configure(final ServiceRegistry registry) {
        final ServiceLocator services = new ServiceLocator(registry);
        this.listeners = new Extensionpoint<>(AgentInitializer.class, registry);
        this.kommando = services.firstMandatory(GenererTidsserieCommand.class);
        this.periode = services.firstMandatory(Observasjonsperiode.class);
    }

    @Override
    public void initialize(final Context<String, Integer> context) {
        // Serienummeret for alle eventar som blir generert for medlemmar i gjeldande partisjon
        serienummer = serienummerGenerator.getAndIncrement();
        notifyListeners(context);
    }

    void notifyListeners(final Context<String, Integer> context) {
        MDC.put(MDC_SERIENUMMER, "" + serienummer);
        listeners.invokeAll(i -> i.partitionInitialized(serienummer))
                .forEachFailure(e -> emitError(context, e));
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
                    periode,
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
                                        + " er registrert i user contexten.\n"
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
