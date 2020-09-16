package no.spk.felles.tidsserie.batch.modus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.Katalog;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.GrunnlagsdataRepository;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.LastOppGrunnlagsdataKommando;
import no.spk.felles.tidsserie.batch.core.grunnlagsdata.csv.CSVInput;
import no.spk.felles.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataOpplaster;
import no.spk.felles.tidsserie.batch.core.medlem.PartisjonsListener;
import no.spk.felles.tidsserie.batch.core.medlem.TidsserieContext;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

public class MedlemsIdModus implements Tidsseriemodus {
    @Override
    public void registerServices(final ServiceRegistry registry) {
        final ServiceLocator services = new ServiceLocator(registry);
        final Path innkatalog = services.firstMandatory(Path.class, Katalog.GRUNNLAGSDATA.egenskap());
        registry.registerService(GrunnlagsdataRepository.class, new CSVInput(innkatalog));

        final MedlemsdataOpplaster overfoering = new MedlemsdataOpplaster();
        registry.registerService(MedlemsdataOpplaster.class, overfoering);
        registry.registerService(LastOppGrunnlagsdataKommando.class, overfoering);

    }

    @Override
    public Map<String, Integer> lagTidsserie(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final StorageBackend storage = locator.firstMandatory(StorageBackend.class);

        final Kommando kommando = new Kommando(storage);
        registry.registerService(GenererTidsserieCommand.class, kommando);
        registry.registerService(PartisjonsListener.class, kommando);

        return genererTidsserie(locator.firstMandatory(MedlemsdataBackend.class));
    }

    private Map<String, Integer> genererTidsserie(final MedlemsdataBackend medlemsdata) {
        return medlemsdata.lagTidsserie();
    }

    @Override
    public String navn() {
        return "medlemsid";
    }

    private static class Kommando implements GenererTidsserieCommand, PartisjonsListener {
        private final StorageBackend storage;

        public Kommando(final StorageBackend storage) {
            this.storage = storage;
        }

        @Override
        public void generer(final String key, final List<List<String>> medlemsdata, final TidsserieContext context) {
            lagre(
                    String.join(
                            ";",
                            key,
                            Thread
                                    .currentThread()
                                    .getName()
                    ) + '\n',
                    context.getSerienummer()
            );
        }

        @Override
        public void partitionInitialized(final long serienummer) {
            lagre("medlemsId;tråd\n", serienummer);
        }

        private void lagre(final String linje, final long serienummer) {
            storage.lagre(
                    e ->
                            e
                                    .reset()
                                    .medFilprefix("medlemsid-")
                                    .serienummer(serienummer)
                                    .medInnhold(linje)
            );
        }
    }
}
