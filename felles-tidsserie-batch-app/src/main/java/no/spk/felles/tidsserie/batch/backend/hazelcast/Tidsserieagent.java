package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.AgentInitializer;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.TidsserieContext;
import no.spk.felles.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
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
 * {@link GenererTidsserieCommand#generer(String, List, TidsserieContext)}   som tar seg av den
 * funksjonelle oppbygginga av tidsseriar.
 *
 * @author Tarjei Skorgenes
 */
class Tidsserieagent
        extends LifecycleMapperAdapter<String, List<List<String>>, String, Integer> implements HazelcastInstanceAware {
    private final static long serialVersionUID = 1;

    public static final String MDC_SERIENUMMER = "serienummer";

    private transient GenererTidsserieCommand kommando;
    private transient Extensionpoint<AgentInitializer> listeners;

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

        context.emit("medlem", 1);

        try {
            kommando.generer(
                    key,
                    value,
                    getTidsserieContext(context)
            );
        } catch (final RuntimeException | Error e) {
            log.warn("Periodisering av medlem {} feila: {} (endringar = {})", key, e.getMessage(), value);
            log.info("Feilkilde:", e);
            emitError(context, e);
        }
    }

    private TidsserieContext getTidsserieContext(final Context<String, Integer> context) {
        return new TidsserieContext() {
            @Override
            public void emitError(Throwable throwable) {
                Tidsserieagent.this.emitError(context, throwable);
            }

            @Override
            public long getSerienummer() {
                return serienummer;
            }
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
