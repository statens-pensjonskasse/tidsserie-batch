package no.spk.tidsserie.batch.modus;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.spk.tidsserie.batch.core.Katalog;
import no.spk.tidsserie.batch.core.lagring.StorageBackend;
import no.spk.tidsserie.batch.core.Tidsseriemodus;
import no.spk.tidsserie.batch.core.grunnlagsdata.GrunnlagsdataRepository;
import no.spk.tidsserie.batch.core.grunnlagsdata.LastOppGrunnlagsdataKommando;
import no.spk.tidsserie.batch.core.grunnlagsdata.csv.CSVInput;
import no.spk.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.tidsserie.batch.core.medlem.MedlemsdataOpplaster;
import no.spk.tidsserie.batch.core.medlem.TidsserieContext;
import no.spk.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Tidsseriemodus som kopierer alt innhold direkte frå {@link GrunnlagsdataRepository#medlemsdata()}
 * og lagrar dei direkte til tidsserieformatet som modusen produserer.
 * <br>
 * Modusen er tiltenkt brukt ved akkseptanse- og integrasjonstesting av platformrammeverket.
 */
public class Medlemsdatakopimodus implements Tidsseriemodus {
    @Override
    public void registerServices(final ServiceRegistry serviceRegistry) {
        final ServiceLocator services = new ServiceLocator(serviceRegistry);
        final Path innkatalog = services.firstMandatory(Path.class, Katalog.GRUNNLAGSDATA.egenskap());
        serviceRegistry.registerService(GrunnlagsdataRepository.class, new CSVInput(innkatalog));
        final MedlemsdataOpplaster overfoering = new MedlemsdataOpplaster();
        serviceRegistry.registerService(MedlemsdataOpplaster.class, overfoering);
        serviceRegistry.registerService(LastOppGrunnlagsdataKommando.class, overfoering);

    }

    @Override
    public Map<String, Integer> lagTidsserie(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final StorageBackend storage = locator.firstMandatory(StorageBackend.class);
        registry.registerService(GenererTidsserieCommand.class, new KopierMedlemsdata(storage));

        genererHeaderlinje(storage);
        return genererTidsserie(locator.firstMandatory(MedlemsdataBackend.class));
    }

    private void genererHeaderlinje(final StorageBackend storage) {
        storage.lagre(e -> e.medInnhold("medlemsdata\n"));
    }

    private Map<String, Integer> genererTidsserie(final MedlemsdataBackend medlemsdata) {
        return medlemsdata.lagTidsserie();
    }

    @Override
    public String navn() {
        return "medlemsdatakopi";
    }

    private static class KopierMedlemsdata implements GenererTidsserieCommand {
        private final StorageBackend storage;

        public KopierMedlemsdata(final StorageBackend storage) {
            this.storage = storage;
        }

        @Override
        public void generer(final String key, final List<List<String>> medlemsdata, final TidsserieContext context) {
            medlemsdata
                    .stream()
                    .map(this::serialiser)
                    .forEach(linje -> lagre(linje, context.getSerienummer()));
        }

        private String serialiser(final List<String> rad) {
            return rad
                    .stream()
                    .map(Optional::ofNullable)
                    .map(cell -> cell.orElse(""))
                    .collect(joining(" "));
        }

        private void lagre(final String linje, final long serienummer) {
            storage.lagre(
                    e ->
                            e
                                    .reset()
                                    .serienummer(serienummer)
                                    .buffer
                                    .append(linje)
                                    .append('\n')
            );
        }
    }
}
