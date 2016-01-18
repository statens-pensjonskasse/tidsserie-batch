package no.spk.pensjon.faktura.tidsserie.plugin.modus.underlagsperioder;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.util.Services.lookupAll;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.core.TidsserieLivssyklus;
import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.plugin.modus.DefaultTidsseriemodusLivssyklus;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LiveTidsseriemodusTest {

    @Rule
    public ServiceRegistryRule services = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    private LiveTidsseriemodus modus;

    @Before
    public void setUp() throws Exception {
        modus = new LiveTidsseriemodus();
        services.registry().registerService(Path.class, Paths.get("."), Katalog.UT.egenskap());
    }

    @Test
    public void skal_annotere_observasjonsunderlaga_med_tidsserienummer_basert_paa_dagens_dato() {
        final LocalDate dato = now();
        final Tidsserienummer expected = Tidsserienummer.genererForDato(dato);
        final List<Underlag> resultat = new ArrayList<>();

        final Observasjonspublikator publikator = modus.nyPublikator(Stream::of, resultat::add);
        publikator.publiser(Stream.of(new Underlag(Stream.empty())));
        assertThat(resultat).as("underlagene mottatt av publikatoren").hasSize(1);

        resultat.forEach(u -> {
            assertThat(u.valgfriAnnotasjonFor(Tidsserienummer.class)).isEqualTo(of(expected));
        });
    }

    @Test
    public void skal_registrere_default_backend_init_callback() {
        modus.registerServices(services.registry());
        assertThat(
                lookupAll(services.registry(), TidsserieLivssyklus.class)
                .filter(l -> l instanceof DefaultTidsseriemodusLivssyklus)
                .findAny()
        ).isPresent();
    }

    @Test
    public void skal_registrere_liveTidsserieAvslutter() {
        modus.registerServices(services.registry());
        assertThat(
                lookupAll(services.registry(), TidsserieLivssyklus.class)
                        .filter(l -> l instanceof LiveTidsserieAvslutter)
                        .findAny()
        ).isPresent();
    }
}