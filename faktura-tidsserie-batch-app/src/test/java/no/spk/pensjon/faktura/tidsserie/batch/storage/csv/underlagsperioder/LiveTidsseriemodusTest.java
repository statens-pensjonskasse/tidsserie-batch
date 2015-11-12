package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.TidsserieResulat;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.storage.disruptor.ObservasjonsEvent;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LiveTidsseriemodusTest {

    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    private LiveTidsseriemodus modus;

    @Before
    public void setUp() throws Exception {
        modus = new LiveTidsseriemodus();
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
    public void skal_skrive_kolonnenavn_naar_partisjon_initisialiseres() {
        final String expected = modus.kolonnenavn().collect(joining(";", "", "\n"));
        assertThat(expected).isNotEmpty();

        final ObservasjonsEvent event = new ObservasjonsEvent();
        modus.partitionInitialized(1, c -> c.accept(event));

        assertThat(event.buffer.toString()).isEqualTo(expected);
    }

    @Test
    public void skal_lage_filer_naar_tidsserie_er_ferdig() throws IOException {
        Path lager = temp.newFolder("skal_lage_filer_naar_tidsserie_er_ferdig").toPath();

        modus.completed(TidsserieResulat.tidsserieResulat(lager).bygg());

        try(Stream<Path> paths = Files.list(lager)) {
            assertThat(paths.findAny()).isPresent();
        }
    }
}