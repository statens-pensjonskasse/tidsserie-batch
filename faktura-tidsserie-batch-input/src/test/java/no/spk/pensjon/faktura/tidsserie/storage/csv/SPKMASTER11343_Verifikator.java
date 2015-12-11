package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractIterableAssert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Ad-hoc test for semi-manuell teamtest av at resultata SPKMASTER-11343 genererer
 * er som forventa i forhold til forrige versjon (1.0.1) av batchen m/gamal domenemodell.
 * <br>
 * Verifiseringa tar inn to CSV-filer, ei generert av gamal løysing, ei generert av ny løysing.
 * <br>
 * Verifikasjonen tar stikkprøver på differansane og verifiserer manuelt at dei alle:
 * <ul>
 * <li>Tilhøyrer eit stillingsforhold som er sluttmeldt på observasjonsdatoen
 * <li>Forskyver nedgangen i årsverk til neste observasjon
 * <li>Forskyver nedgangen i grunnlag til neste observasjon
 * </ul>
 * <br>
 * For å køyre testen må du manuelt generere to datasett, ein med versjonen du ønskjer å samanlikne mot
 * og ein med versjonen som inneheld endringane du ønskjer å teste.
 * <br>
 * Plasser tidsserien på aggregert stillingsforholdnivå i fila gammal-versjon.csv og ny-versjon.csv i modulens
 * rotkatalog og køyr testen.
 *
 * @author Tarjei Skorgenes
 */
public class SPKMASTER11343_Verifikator {
    private Path gamalVersjon = Paths.get("gammal-versjon.csv");
    private Path nyVersjon = Paths.get("ny-versjon.csv");
    private Map<Object, Linje> old;
    private Map<Object, Linje> ny;
    private List<LinjeDiff> forskjellar;

    @Before
    public void _before() throws IOException {
        Assume.assumeTrue(gamalVersjon.toFile().exists());
        Assume.assumeTrue(nyVersjon.toFile().exists());

        old = group(gamalVersjon);
        ny = group(nyVersjon);

        forskjellar = old
                .entrySet()
                .stream()
                .map(e -> new LinjeDiff(e.getValue(), ny.get(e.getKey())))
                .filter(LinjeDiff::erForskjellige)
                .collect(toList());
    }

    /**
     * SPKMASTER-11343 skal medføre endringar mellom dei to filene.
     */
    @Test
    public void skalInneholdeForskjellar() {
        assertThat(forskjellar).isNotEmpty();
    }

    /**
     * Verifiserer at alle observasjonane som er i gamal versjon er i ny versjon og vice-versa, dvs ingen nye eller fjerna
     * observasjonar mellom dei to versjonane.
     */
    @Test
    public void skalInneholdeEksaktSammeObservasjonsnoekklar() throws IOException {
        assertDisjunction(old, ny)
                .as("nøklar for rader som er i gamal fil men ikkje i ny")
                .isEmpty();

        assertDisjunction(ny, old)
                .as("nøklar for rader som er i ny fil men ikkje i gamal")
                .hasSize(0);
    }

    /**
     * Verifiserer at årsverkverdien er størst i ny versjon for alle observasjonane som er forskjellige i dei to datasetta.
     */
    @Test
    public void skalHaStoerreAarsverkverdiiNyVersjonPaaAlleObservasjonarSomErForskjellige() {
        assertThat(
                forskjellar
                        .stream()
                        .filter(d -> parseDouble(d.ny.årsverk) - parseDouble(d.old.årsverk) <= 0d)
                        .collect(toList())
        )
                .as("målingar der nye årsverk _ikkje_ er større enn dei gamle")
                .isEmpty();
    }

    /**
     * Verifiserer at grunnlag er størst i ny versjon for alle observasjonane som er forskjellige i dei to datasetta.
     * <br>
     * For alle stillingar som den feilar på kan du bruke følgjande SQL mot databasen datane er henta frå for å sjekke manuelt om siste historikkrad indikerer at stillinga er ute i permisjon utan lønn eller er under minstegrensa fram til den sluttar.
     * <code>
     * SELECT * FROM TORT016 s WHERE FLG_SLETTET = 0
     * AND IDE_SEKV_TORT125 IN( legg til stillingaforholda frå diffen her... )
     * AND IDE_LINJE_NR = (SELECT IDE_LINJE_NR_FORRIGE FROM TORT016 s2 WHERE FLG_SLETTET = 0 AND s.IDE_SEKV_TORT125 = s2.IDE_SEKV_TORT125 AND TYP_AKSJONSKODE = '031')
     * ORDER BY DAT_AKSJON DESC
     * <p>
     * <p>
     * </code>
     */
    @Test
    public void skalHaStoerreGrunnlagsverdiiNyVersjonPaaAlleObservasjonarSomErForskjellige() {
        assertThat(
                forskjellar
                        .stream()
                        .filter(LinjeDiff::harMinstEtGrunnlagOverKroner0)
                        .filter(d -> parseDouble(d.ny.grunnlag) - parseDouble(d.old.grunnlag) <= 0d)
                        .collect(toList())
        )
                .as("målingar der nye grunnlag  _ikkje_ er større enn dei gamle")
                .isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static AbstractIterableAssert<?, ? extends Iterable<?>, Object> assertDisjunction(Map<Object, Linje> a, Map<Object, Linje> b) {
        final Set<Object> tmp = new HashSet<>(a.keySet());
        tmp.removeAll(b.keySet());
        return assertThat((Iterable)tmp);
    }

    public static Map<Object, Linje> group(Path old) throws IOException {
        try (final Stream<String> lines = Files.lines(old, Charset.forName("CP1252"))) {
            return lines
                    .filter(line -> !line.startsWith("avtale"))
                    .map(Linje::new)
                    .collect(
                            groupingBy(
                                    Linje::key,
                                    collectingAndThen(
                                            Collectors.reducing((a, b) -> {
                                                throw new RuntimeException(a.toString());
                                            }),
                                            Optional::get
                                    )
                            )
                    );
        }
    }

    private static class LinjeDiff {
        private final Linje old;
        private final Linje ny;

        private LinjeDiff(Linje old, Linje ny) {
            this.old = old;
            this.ny = ny;
        }

        boolean erForskjellige() {
            return !old.line().equals(ny.line());
        }

        @Override
        public String toString() {
            return "-" + old.line() + "\n" + "+" + ny.line() + "\n";
        }

        public boolean harMinstEtGrunnlagOverKroner0() {
            return !(old.grunnlag.equals("0") && ny.grunnlag.equals("0"));
        }
    }

    private static class Linje {
        private final String line;
        String avtale;
        String stilling;
        String observasjonsdato;
        String årsverk;
        String grunnlag;

        public Linje(final String line) {
            this.line = line;
            final String[] columns = line.split(";");
            avtale = columns[0];
            stilling = columns[1];
            observasjonsdato = columns[2];
            årsverk = columns[5];
            grunnlag = columns[3];
        }

        public String line() {
            return line;
        }

        @Override
        public String toString() {
            return line;
        }

        public Object key() {
            return avtale + ";" + stilling + ";" + observasjonsdato;
        }
    }
}
