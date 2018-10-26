package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

/**
 * {@link AldersgrenseForSlettingAvLogKatalogar} representerer grenseverdien for kor gamal ein logkatalog kan
 * vere før den blir sletta automatisk.
 * <p>
 * Aldersgrensa er relativ til midnatt ved {@link LocalDate#now() dagens dato}, logkatalogar oppretta før
 * dette tidspunktet blir ikkje sletta automatisk.
 *
 * @since 1.1.0
 */
public class AldersgrenseForSlettingAvLogKatalogar {
    private final int antallDagar;
    private final LocalDateTime cutoff;

    private AldersgrenseForSlettingAvLogKatalogar(final int antallDagar) {
        if (antallDagar < 0) {
            throw new IllegalArgumentException(
                    format(
                            "Antall dagar kan ikkje ha ein negativ verdi, var %d",
                            antallDagar
                    )
            );
        }
        this.antallDagar = antallDagar;
        this.cutoff = cutoff(LocalDate.now());
    }

    LocalDateTime cutoff() {
        return cutoff;
    }

    LocalDateTime cutoff(final LocalDate dato) {
        return dato.atStartOfDay().plusDays(1).minusDays(antallDagar);
    }

    /**
     * Opprettar ein ny aldersgrense for sletting av logkatalogar.
     *
     * @param antallDagar aldersgrensa som regulerer kor gamle logkatalogane må vere før dei kan bli sletta
     * @return ei ny aldersgrense for sletting av logkatalogar
     * @throws IllegalArgumentException dersom <code>antallDagar</code> er mindre enn 0 dagar
     */
    public static AldersgrenseForSlettingAvLogKatalogar aldersgrenseForSlettingAvLogKatalogar(final int antallDagar) {
        return new AldersgrenseForSlettingAvLogKatalogar(antallDagar);
    }

    /**
     * Køyrer sletteoperasjonen dersom aldersgrensa er større en 0 dagar.
     *
     * @param sletting operasjonen som vil slette logkatalogar som er eldre enn aldersgrensa
     * @throws IOException dersom slettinga feilar
     */
    public Stream<Path> finnSlettbareLogkatalogar(final FinnLogkatalogerOperasjon sletting) throws IOException {
        if (antallDagar > 0) {
            return sletting.finn(cutoff());
        }
        return Stream.empty();
    }

    public interface FinnLogkatalogerOperasjon {
        /**
         * Lokaliserer alle logkatalogar som er eldre enn <code>cutoff</code>.
         *
         * @param cutoff klokkeslett som logkatalogar som skal slettast må vere eldre enn
         * @throws IOException dersom søkeoperasjonen feilar
         */
        Stream<Path> finn(final LocalDateTime cutoff) throws IOException;
    }
}