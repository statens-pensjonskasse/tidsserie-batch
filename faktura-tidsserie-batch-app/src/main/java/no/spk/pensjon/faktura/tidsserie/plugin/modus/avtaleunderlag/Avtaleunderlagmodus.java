package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.GRUNNLAGSDATA_PREFIX;
import static no.spk.pensjon.faktura.tidsserie.util.Services.lookup;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import no.spk.faktura.input.BatchId;
import no.spk.pensjon.faktura.tidsserie.batch.main.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Avtaleunderlagmodus lager underlagsperioder for avtaler.
 * @author Snorre E. Brekke - Computas
 */
public class Avtaleunderlagmodus implements Tidsseriemodus {

    private final CSVFormat outputFormat = new Avtaleunderlagformat();
    private final Regelsett regler = new AvtaleunderlagRegelsett();

    private Optional<Avtaleunderlagskriver> avtaleunderlagskriver = Optional.empty();

    @Override
    public void registerServices(ServiceRegistry serviceRegistry) {
        final GrunnlagsdataService grunnlagsdata = lookup(serviceRegistry, GrunnlagsdataService.class);
        final AvtaleunderlagFactory underlagFactory = new AvtaleunderlagFactory(grunnlagsdata, regelsett());
        serviceRegistry.registerService(AvtaleunderlagFactory.class, underlagFactory);
    }

    @Override
    public Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder) {
        return Stream.empty();
    }

    @Override
    public Stream<String> kolonnenavn() {
        return outputFormat.kolonnenavn();
    }

    @Override
    public Regelsett regelsett() {
        return regler;
    }

    @Override
    public Observasjonspublikator createPublikator(TidsserieFacade tidsserie, long serienummer, StorageBackend storage) {
        return o -> {};
    }

    @Override
    public Map<String, Integer> lagTidsserie(ServiceRegistry registry) {
        final Observasjonsperiode observasjonsperiode = lookup(registry, Observasjonsperiode.class);
        final AvtaleunderlagFactory factory = lookup(registry, AvtaleunderlagFactory.class);
        final FileTemplate fileTemplate = lookup(registry, FileTemplate.class);

        final Stream<Underlag> underlag = factory.lagAvtaleunderlag(observasjonsperiode, uttrekksdato(registry));

        lagreUnderlag(fileTemplate, underlag);

        return new HashMap<>();
    }

    private Uttrekksdato uttrekksdato(ServiceRegistry registry) {
        final Path grunnlagsdata = lookup(registry, Path.class, Katalog.GRUNNLAGSDATA.egenskap());
        final LocalDate untrekksdato = BatchId.fromString(GRUNNLAGSDATA_PREFIX, grunnlagsdata.getFileName().toString())
                .asLocalDateTime()
                .toLocalDate();
        return new Uttrekksdato(untrekksdato);
    }

    private void lagreUnderlag(FileTemplate fileTemplate, Stream<Underlag> underlag) {
        avtaleunderlagskriver
                .orElse(new Avtaleunderlagskriver(fileTemplate, outputFormat))
                .skrivAvtaleunderlag(underlag);
    }

    void avtaleunderlagsskriver(Avtaleunderlagskriver avtaleunderlagskriver) {
        this.avtaleunderlagskriver = Optional.of(avtaleunderlagskriver);
    }

}
