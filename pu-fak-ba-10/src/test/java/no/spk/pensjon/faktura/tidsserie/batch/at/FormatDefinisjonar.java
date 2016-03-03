package no.spk.pensjon.faktura.tidsserie.batch.at;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Datavarehusformat;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;
import no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie.Avregningformat;

import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java8.No;

/**
 * @author Snorre E. Brekke - Computas
 */
public class FormatDefinisjonar implements No {

    private CSVFormat format;

    private UnderlagsperiodeBuilder periode;

    private Set<String> kolonner;

    public FormatDefinisjonar() {

        Gitt("^en villkårlig underlagsperiode$", () -> {
        });


        Når("^underlaget formateres med (.+)$", (String formatnavn) -> {
            format = lagCSVFormat(formatnavn);
            kolonner = format.kolonnenavn().collect(toSet());
        });


        Når("^kun følgende kolonner beregnes:$", (DataTable kolonnenavn) -> {
            kolonner = new LinkedHashSet<>(kolonnenavn.asList(String.class).stream().skip(1).collect(toList()));
        });

        Så("^blir resultatet med angitt format:$", (DataTable forventetResultat) -> {
            final Underlagsperiode underlagsperiode = periode.bygg();
            Underlag underlag = new Underlag(Stream.of(underlagsperiode));

            final Iterator<Object> verdier = format.serialiser(underlag, underlagsperiode, kolonner).iterator();

            final Map<String, Object> resultat = kolonner.stream()
                    .collect(
                            toMap(
                                    k -> k,
                                    k -> verdier.next()
                            )
                    );

            forventetResultat
                    .asMaps(String.class, String.class)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .forEach(forventet ->
                            assertThat(
                                    resultat.get(forventet.getKey())
                            )
                                    .as("verdi for kolonne " + forventet.getKey())
                                    .isEqualTo(forventet.getValue())
                    );
        });
    }

    private CSVFormat lagCSVFormat(String format) {
        switch (format) {
        case "avregningsformat":
            return new Avregningformat();
        case "live-tidsserieformat":
            return new Datavarehusformat();
        default:
            throw new IllegalArgumentException("Ukjent format: " + format);
        }
    }

    @Before
    public void nyPeriode() {
        final Aarstall premieAar = new Aarstall(now().getYear());
        periode = new UnderlagsperiodeBuilder()
                .fraOgMed(premieAar.atStartOfYear())
                .tilOgMed(premieAar.atEndOfYear());
    }
}
