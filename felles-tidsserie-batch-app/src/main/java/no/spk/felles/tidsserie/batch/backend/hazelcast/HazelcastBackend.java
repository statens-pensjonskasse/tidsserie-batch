package no.spk.felles.tidsserie.batch.backend.hazelcast;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

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
 * Medlemsdatabackend som lar ein laste opp medlemsdata til eit in-memory datagrid
 * i Hazelcast.
 * <br>
 * Sjølv om Hazelcast støttar distribuert prosessering på tvers av prosessar og maskiner,
 * har batchen foreløpig kun implementert støtte for å spinne opp 1 eller fleire hazelcast-noder
 * innanfor ein og samme JVM. Dette er gjort fordi klientane av batchen hittil ikkje har hatt
 * behov for reell distribuert multi-maskin/multi-node prosessering.
 * <br>
 * Ettersom backenden vil laste opp alle medlemsdata in-memory må derfor applikasjonen
 * som benyttar seg av backenden passe på å ha tilstrekkelig heap-kapasitet slik at heile datasettet
 * får plass på heapen.
 * <br>
 * Beregningsagentane som {@link HazelcastBackend} sender ut i gridet ved kall til {@link #lagTidsserie()} vil typisk
 * vere avhengige fellestenester for lagring av resultat og innhenting av avtale- og referansedata. For å gi dei tilgang
 * til slike tenester må alle fellestenester som skal benyttast av agentane eller tenester dei delegerer til, ha blitt
 * registrert i {@link ServiceRegistry tjenesteregisteret} før {@link #lagTidsserie()} blir kalla.
 * <br>
 * Den funksjonelle implementasjonen av tidsseriegenereringa blir delegert til standardtenesta av type
 * {@link GenererTidsserieCommand}. Den må derfor ha blitt registrert i tenesteregisteret i forkant av at
 * {@link #lagTidsserie()} blir kalla.
 */
public class HazelcastBackend implements MedlemsdataBackend, TidsserieLivssyklus {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Server server;

    private Optional<IMap<String, List<List<String>>>> map = empty();

    private Optional<HazelcastInstance> instance = empty();

    public HazelcastBackend(final ServiceRegistry registry, final int antallNoder) {
        this(
                new MultiNodeSingleJVMBackend(
                        registry,
                        antallNoder
                )
        );
    }

    HazelcastBackend(final Server server) {
        this.server = requireNonNull(server, "server er påkrevd, men var null");
    }

    @Override
    public void stop(final ServiceRegistry registry) {
        server.stop();
    }

    @Override
    public void start() {
        this.instance = of(server.start());
        this.map = instance.map(i -> i.getMap("medlemsdata"));
        instance.ifPresent(i -> i.getAtomicLong("serienummer").set(1L));
    }

    @Override
    public MedlemsdataUploader uploader() {
        return new UploadCommand(this.map.get());
    }

    @Override
    public Map<String, Integer> lagTidsserie() {
        return submit(new Tidsserieagent());
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
            return future.get();
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
