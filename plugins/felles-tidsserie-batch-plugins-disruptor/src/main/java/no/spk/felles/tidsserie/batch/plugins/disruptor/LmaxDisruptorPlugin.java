package no.spk.felles.tidsserie.batch.plugins.disruptor;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus.onStop;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class LmaxDisruptorPlugin implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);
        // Lagring
        final ThreadFactory threadFactory = r -> new Thread(r, "lmax-disruptor-" + System.currentTimeMillis());

        final ExecutorService executors = newCachedThreadPool(threadFactory);
        registry.registerService(ExecutorService.class, executors);
        registry.registerService(TidsserieLivssyklus.class, onStop(executors::shutdown));

        final Path utKatalog = locator.firstMandatory(Path.class, Katalog.UT.egenskap());
        final LmaxDisruptorPublisher disruptor = new LmaxDisruptorPublisher(
                threadFactory,
                new FileTemplate(utKatalog, ".csv")
        );
        registry.registerService(StorageBackend.class, disruptor);
        registry.registerService(TidsserieLivssyklus.class, disruptor);
    }
}
