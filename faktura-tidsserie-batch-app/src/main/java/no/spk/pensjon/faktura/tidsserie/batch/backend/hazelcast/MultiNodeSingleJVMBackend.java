package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static com.hazelcast.instance.HazelcastInstanceFactory.newHazelcastInstance;
import static java.lang.System.setProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.DefaultNodeContext;
import com.hazelcast.instance.GroupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MultiNodeSingleJVMBackend} handterer og kontrollerer Hazelcast-gridet som tidsseriane
 * blir generert via.
 * <br>
 * Backenden startar opp eit brukar-kontrollerbart antall beregningsnoder som inngår i eit delt datagrid. Griddet
 * blir satt opp med eit unikt cluster-navn og med innstillingar som skal sikre at det kun er nodene som køyrer
 * innanfor den aktive JVMen som vil bli brukt av tidsseriegenereringa.
 * <br>
 * Gridet består av ei master-node og N-1 slave-noder der N blir styrt via kommandolinjeargument ved oppstart av
 * batchen.
 * <br>
 * For å redusere minnebruken for medlemsdatane tidsserien skal genererast ut i frå, blir alle medlemsdata lagra i
 * binært-format, ikkje i serialisert form.
 * <br>
 * Sidan beregningsagentane som {@link no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend} sender
 * ut i gridet også er avhengig av fellestenester for lagring av resultat og innhenting av avtale- og referansedata,
 * tilbyr klassa muligheiter for å registrere fellestenester i kvar enkelt node sin
 * {@link com.hazelcast.core.HazelcastInstance#getUserContext()}.
 * <br>
 * Hazelcast blir satt opp til å rute alle logginnslag via slf4j.
 *
 * @author Tarjei Skorgenes
 */
class MultiNodeSingleJVMBackend implements Server {
    private final Logger logger = LoggerFactory.getLogger(MultiNodeSingleJVMBackend.class);
    ;

    private final Set<HazelcastInstance> slavar = new HashSet<>();

    private final int antallNoder;

    private Optional<HazelcastInstance> master = empty();

    public MultiNodeSingleJVMBackend(int antallNoder) {
        this.antallNoder = antallNoder;
    }

    /**
     * Startar opp master- og slavenodene.
     * <br>
     * Etter at metoda returnerer vil gridet vere klart til å behandle og ta vare på data opplasta via
     * {@link HazelcastBackend#uploader()}.
     *
     * @return masternoda
     */
    @Override
    public HazelcastInstance start() {
        setProperty("hazelcast.logging.type", "slf4j");

        final Config config = new XmlConfigBuilder().build();
        config.setProperty(GroupProperties.PROP_INITIAL_MIN_CLUSTER_SIZE, "1");
        config.setProperty(GroupProperties.PROP_SOCKET_BIND_ANY, "false");
        config.getGroupConfig().setName("faktura-prognose-tidsserie-" + UUID.randomUUID().toString());
        config
                .getMapConfig("medlem")
                .setEvictionPolicy(EvictionPolicy.NONE)
                .setInMemoryFormat(InMemoryFormat.BINARY);
        config
                .getSerializationConfig()
                .addSerializerConfig(
                        new SerializerConfig()
                                .setTypeClass(Endringer.class)
                                .setImplementation(new IterableSerializer()
                                )
                );
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
        config.getNetworkConfig().getInterfaces().addInterface("127.0.0.1");


        logger.info("Startar masternoda...");
        master = of(startInstance(config, 1));

        logger.info("Startar sekundærnoder...");
        slavar.addAll(
                IntStream
                        .rangeClosed(2, antallNoder)
                        .parallel()
                        .mapToObj(threadNr -> startInstance(config, threadNr))
                        .collect(toSet())
        );
        logger.info("Alle beregningsnoder har starta");
        return master.get();
    }

    /**
     * Registrerer tenesta angitt via <code>service</code> under tenestenavnet angitt av <code>serviceType</code>
     * i master- og slavenodenes {@link com.hazelcast.core.HazelcastInstance#getUserContext() usercontext}.
     *
     * @param <T>         tenestetypen som blir registrert
     * @param serviceType kva tenestetype tenesta skal registrerast som. Det forventast at tenesta kan castast til
     *                    denne typen av klientane som slår den opp frå usercontexten seinare
     * @param service     tenesta som skal registrerast under det angitte tenestenavnet i usercontexten til alle nodene
     */
    @Override
    public <T> void registrer(final Class<T> serviceType, final T service) {
        final String key = serviceType.getSimpleName();
        master.orElseThrow(
                () -> new IllegalStateException("Registrering av tenester kan ikkje utførast før etter at Hazelcast-backenden har blitt starta")
        ).getUserContext().put(key, service);
        slavar.forEach(slave -> slave.getUserContext().put(key, service));
    }

    private static HazelcastInstance startInstance(final Config config, final int instanceNr) {
        return newHazelcastInstance(
                config,
                "faktura-prognose-tidsserie-" + instanceNr,
                new DefaultNodeContext()
        );
    }
}
