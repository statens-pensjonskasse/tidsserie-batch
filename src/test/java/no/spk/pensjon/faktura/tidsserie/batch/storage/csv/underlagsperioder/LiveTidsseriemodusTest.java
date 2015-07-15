package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;

import org.junit.Test;

public class LiveTidsseriemodusTest {
    @Test
    public void skalAnnotereObservasjonsunderlagaMedTidsserienummerBasertPaaDagensDato() {
        final LocalDate dato = now();
        final Tidsserienummer expected = Tidsserienummer.genererForDato(dato);
        final List<Underlag> resultat = new ArrayList<>();

        final Observasjonspublikator publikator = new LiveTidsseriemodus().nyPublikator(Stream::of, resultat::add);
        publikator.publiser(Stream.of(new Underlag(Stream.empty())));
        assertThat(resultat).as("underlagene mottatt av publikatoren").hasSize(1);

        resultat.forEach(u -> {
            assertThat(u.valgfriAnnotasjonFor(Tidsserienummer.class)).isEqualTo(of(expected));
        });
    }
}