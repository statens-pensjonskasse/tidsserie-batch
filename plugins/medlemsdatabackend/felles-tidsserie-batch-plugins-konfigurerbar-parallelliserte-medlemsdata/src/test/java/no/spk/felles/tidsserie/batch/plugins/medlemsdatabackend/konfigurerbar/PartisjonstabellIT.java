package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.medlemsdata;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Nodenummer.nodenummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.Medlemsdata;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class PartisjonstabellIT {
    private final Partisjonstabell partisjonstabell = new Partisjonstabell();

    @Test
    public void skal_fordele_medlemsdata_ut_over_partisjonane_uten_at_variasjonen_i_antall_medlemmar_pr_partisjon_blir_disproposjonalt_høg() {
        final int antallMedlemmar = 500_000;
        final int antallPersonarPrFødselsdato = 11;
        final String førsteFødselsdato = "1917-01-01";

        final Set<Partisjon> partisjonar = populerPartisjonstabell(
                generertRandomiserteMedlemmar(
                        antallMedlemmar,
                        antallPersonarPrFødselsdato,
                        førsteFødselsdato
                )
        );

        final SoftAssertions softly = new SoftAssertions();
        assertThat(
                prPartisjonsnummer(partisjonar)
        )
                .allSatisfy(
                        (partisjonsnummer, partisjon) ->
                                softly.assertThat(partisjon.size())
                                        .as("antall medlemmar i partisjon %s", partisjon)
                                        .isBetween(
                                                (int) Math.round(antallMedlemmar / 271d * (1d - toleransegrense())),
                                                (int) Math.round(antallMedlemmar / 271d * (1d + toleransegrense()))
                                        )
                );
        softly.assertAll();
    }

    // Den maksimale variasjonen vi til godta i antall medlemmar pr partisjon blir 2x verdien vi velger her
    // 7% toleransegrense er valgt ut frå "finger i været"-metoda.
    private double toleransegrense() {
        return 0.07d;
    }

    private Set<Partisjon> populerPartisjonstabell(final Stream<String> medlemmar) {
        final Medlemsdata tommeMedlemsdata = medlemsdata(rad());
        medlemmar
                .forEach(
                        medlemsId ->
                                partisjonstabell.put(
                                        medlemsId,
                                        tommeMedlemsdata.medlemsdata(),
                                        new DefaultDatalagringStrategi()
                                )
                );

        final Set<Partisjon> partisjonar = partisjonstabell.partisjonarFor(nodenummer(1, 1));
        assertThat(partisjonar).hasSize(271);
        return partisjonar;
    }

    private static Stream<String> generertRandomiserteMedlemmar(
            final int antallMedlemmar,
            final int antallPersonarPrFødselsdato,
            final String førsteFødselsdato
    ) {
        final Random rekkefølge = new Random();
        return Stream
                .iterate(
                        LocalDate.parse(førsteFødselsdato),
                        fødselsdato -> fødselsdato.plusDays(1)
                )
                .flatMap(
                        fødselsdato ->
                                IntStream
                                        .iterate(0, personnummer -> personnummer + 100_000 / antallPersonarPrFødselsdato)
                                        .limit(antallPersonarPrFødselsdato)
                                        .mapToObj(
                                                personnummer ->
                                                        format(
                                                                "%s%05d",
                                                                fødselsdato
                                                                        .toString()
                                                                        .replace("-", ""),
                                                                personnummer
                                                        )
                                        )
                )
                .map(medlemsId -> new Medlem(rekkefølge.nextInt(), medlemsId))
                .limit(antallMedlemmar)
                .sorted(comparing((Medlem m) -> m.rekkefølge))
                .map(Medlem::toString);
    }

    private static Map<Partisjonsnummer, Partisjon> prPartisjonsnummer(final Set<Partisjon> partisjonar) {
        return partisjonar
                .stream()
                .filter(p -> !p.isEmpty())
                .collect(toMap(Partisjon::nummer, Function.identity()));
    }

    private static class Medlem {
        final int rekkefølge;
        final String medlemsId;

        private Medlem(final int rekkefølge, final String medlemsId) {
            this.rekkefølge = rekkefølge;
            this.medlemsId = medlemsId;
        }

        @Override
        public String toString() {
            return medlemsId;
        }
    }
}