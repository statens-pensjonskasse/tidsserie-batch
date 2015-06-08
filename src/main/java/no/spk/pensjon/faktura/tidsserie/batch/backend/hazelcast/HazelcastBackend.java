package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.Optional.of;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import no.spk.pensjon.faktura.tidsserie.batch.MedlemsdataUploader;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

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

    private final Server server = new Server();

    private Optional<IMap<String, List<List<String>>>> map;

    private Optional<HazelcastInstance> instance;

    @Override
    public void start() {
        this.instance = of(server.start());
        this.map = instance.map(i -> i.getMap("medlemsdata"));
    }

    @Override
    public MedlemsdataUploader uploader() {
        return new UploadCommand(server, this.map.get());
    }

    @Override
    public Map<String, Integer> lagTidsseriePaaStillingsforholdNivaa(
            final File outputFiles, final Aarstall fraOgMed, final Aarstall tilOgMed) {
        return submit(
                new GenererTidsseriePrStillingsforholdOgAar(
                        outputFiles,
                        fraOgMed.atStartOfYear(),
                        tilOgMed.atEndOfYear()
                )
        );
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


        log.info("Startar køyring av {}", mapper);
        try {

            final Map<String, Integer> resultat = future.get();
            log.info("Resultat av køyring: {}", resultat);
            return resultat;
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Køyring fullført på {} ms", System.currentTimeMillis() - start);
        }
    }

    private Job<String, List<List<String>>> createJob() {
        final JobTracker tracker = instance.get().getJobTracker("default");
        final KeyValueSource<String, List<List<String>>> source = KeyValueSource.fromMap(map.get());
        return tracker.newJob(source);
    }
}
