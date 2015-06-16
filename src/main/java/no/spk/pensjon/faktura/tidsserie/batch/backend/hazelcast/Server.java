package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static com.hazelcast.instance.HazelcastInstanceFactory.newHazelcastInstance;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.IntStream;

import no.spk.pensjon.faktura.tidsserie.batch.TidsserieFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.DefaultNodeContext;
import com.hazelcast.instance.GroupProperties;
import org.slf4j.LoggerFactory;

class Server {

    private HazelcastInstance master;
    private Set<HazelcastInstance> slavar;

    public HazelcastInstance start() {
        setProperty("hazelcast.logging.type", "slf4j");

        final Config config = new XmlConfigBuilder().build();
        config.setProperty(GroupProperties.PROP_INITIAL_MIN_CLUSTER_SIZE, "1");
        config.setProperty(GroupProperties.PROP_SOCKET_BIND_ANY, "false");
        config.getGroupConfig().setName("faktura-prognose-tidsserie-" + getProperty("user.name", "local"));
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

        LoggerFactory.getLogger(Server.class).info("Startar masternoda...");
        master = startInstance(config, 1);

        LoggerFactory.getLogger(Server.class).info("Startar sekundærnoder...");
        slavar = IntStream
                .range(2, Runtime.getRuntime().availableProcessors())
                .parallel()
                .mapToObj(threadNr -> startInstance(config, threadNr))
                .collect(toSet());
        LoggerFactory.getLogger(Server.class).info("Alle beregningsnoder har starta");
        return master;
    }

    private static HazelcastInstance startInstance(final Config config, final int instanceNr) {
        return newHazelcastInstance(
                config,
                "faktura-prognose-tidsserie-" + instanceNr,
                new DefaultNodeContext()
        );
    }

    public void registrer(final TidsserieFactory service) {
        String key = TidsserieFactory.class.getSimpleName();
        master.getUserContext().put(key, service);
        slavar.forEach(slave -> slave.getUserContext().put(key, service));
    }
}
