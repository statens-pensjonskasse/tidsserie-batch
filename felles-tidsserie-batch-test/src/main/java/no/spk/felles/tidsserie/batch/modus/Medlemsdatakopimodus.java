package no.spk.felles.tidsserie.batch.modus;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.core.GrunnlagsdataRepository;
import no.spk.felles.tidsserie.batch.core.ServiceLocator;
import no.spk.felles.tidsserie.batch.core.StorageBackend;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
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
        public void generer(final List<List<String>> medlemsdata, final Observasjonsperiode periode, final Feilhandtering feilhandtering, final long serienummer) {
            medlemsdata
                    .stream()
                    .map(this::serialiser)
                    .forEach(this::lagre);
        }

        private String serialiser(final List<String> rad) {
            return rad
                    .stream()
                    .map(Optional::ofNullable)
                    .map(cell -> cell.orElse(""))
                    .collect(joining(" "));
        }

        private void lagre(final String linje) {
            storage.lagre(e -> e.reset().buffer.append(linje).append('\n'));
        }
    }
}
