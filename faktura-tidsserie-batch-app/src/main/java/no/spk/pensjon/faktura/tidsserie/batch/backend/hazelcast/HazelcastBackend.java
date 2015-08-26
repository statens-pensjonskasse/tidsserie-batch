package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import no.spk.pensjon.faktura.tidsserie.batch.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.batch.MedlemsdataUploader;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.disruptor.LmaxDisruptorPublisher;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.mapreduce.Job;
import com.hazelcast.mapreduce.JobCompletableFuture;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.KeyValueSource;
import com.hazelcast.mapreduce.Mapper;
import com.hazelcast.mapreduce.aggregation.impl.IntegerSumAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gode JVM-argument ved bruk av denne backenden:
 * <pre>
 * -server
 * -Xms8000m
 * -Xmx8000m
 * -XX:NewSize=4000m
 * -XX:MaxNewSize=4000m
 * -XX:SurvivorRatio=36
 * -XX:+UseCompressedOops
 * -XX:+PrintTenuringDistribution
 * -XX:+UseTLAB
 * -XX:+UseParNewGC
 * -verbose:gc
 * </pre>
 */
public class HazelcastBackend implements TidsserieBackendService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Server server;
    private final Tidsseriemodus parameter;

    private Optional<IMap<String, List<List<String>>>> map = empty();

    private Optional<HazelcastInstance> instance = empty();

    public HazelcastBackend(final int antallNoder, final Tidsseriemodus parameter) {
        this(new MultiNodeSingleJVMBackend(antallNoder), parameter);
    }

    HazelcastBackend(final Server server, final Tidsseriemodus parameter) {
        this.server = requireNonNull(server, "server er p�krevd, men var null");
        this.parameter = requireNonNull(parameter, "tidsserieparameter er p�krevd, men var null");
    }

    @Override
    public void start() {
        this.instance = of(server.start());
        this.map = instance.map(i -> i.getMap("medlemsdata"));
        instance.ifPresent(i -> i.getAtomicLong("serienummer").set(1L));
        registrer(Tidsseriemodus.class, parameter);
    }

    @Override
    public MedlemsdataUploader uploader() {
        return new UploadCommand(this.map.get());
    }

    @Override
    public Map<String, Integer> lagTidsseriePaaStillingsforholdNivaa(
            final FileTemplate outputFiles, final Aarstall fraOgMed, final Aarstall tilOgMed) {
        final ExecutorService executors = newCachedThreadPool(
                r -> new Thread(r, "lmax-disruptor-" + System.currentTimeMillis())
        );
        try (final LmaxDisruptorPublisher lager = openDisruptor(executors, outputFiles)) {
            return submit(
                    new Tidsserieagent(
                            fraOgMed.atStartOfYear(),
                            tilOgMed.atEndOfYear()
                    )
            );
        } finally {
            executors.shutdown();
        }
    }

    @Override
    public <T> void registrer(final Class<T> serviceType, final T service) {
        server.registrer(serviceType, service);
    }

    private LmaxDisruptorPublisher openDisruptor(final ExecutorService executors, final FileTemplate fileTemplate) {
        final LmaxDisruptorPublisher publisher = new LmaxDisruptorPublisher(executors, fileTemplate);
        publisher.start();
        registrer(StorageBackend.class, publisher);
        return publisher;
    }

    private void publishHeader(final LmaxDisruptorPublisher lager) {
        lager.lagre(event -> {
            event.buffer
                    .append(parameter.kolonnenavn().collect(joining(";")))
                    .append('\n')
            ;
        });
    }

    private Map<String, Integer> submit(
            final Mapper<String, List<List<String>>, String, Integer> mapper
    ) {
        final IntegerSumAggregation<String, Integer> factory = new IntegerSumAggregation<>();
        final JobCompletableFuture<Map<String, Integer>> future = createJob()
                .mapper(mapper)
                .combiner(factory.getCombinerFactory())
                .reducer(factory.getReducerFactory())
                .submit();
        long start = System.currentTimeMillis();


        log.info("Startar k�yring av {}", mapper);
        try {

            final Map<String, Integer> resultat = future.get();
            log.info("Resultat av k�yring: {}", resultat);
            return resultat;
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("K�yring fullf�rt p� {} ms", System.currentTimeMillis() - start);
        }
    }

    private Job<String, List<List<String>>> createJob() {
        final JobTracker tracker = instance.get().getJobTracker("default");
        final KeyValueSource<String, List<List<String>>> source = KeyValueSource.fromMap(map.get());
        return tracker.newJob(source);
    }
}
