package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.Datoar;

/**
 * Støttemetoder for innlesing av og konvertering av tekstfelt brukt i oversettinga frå medlemsdata til
 * periodetyper.
 */
class OversetterSupport {
    /**
     * Hentar ut og konverterer ein datoverdi frå den angitte indeksen i rada.
     * <p>
     * Det foretrukne dato-formatet er på format yyyy.MM.dd, men formatet yyyy-MM-dd er også støtta. Dersom
     * datoverdien er lengre enn 10 tegn blir kun dei 10 første forsøkt innlest, eventuelle tidsverdiar blir
     * dermed ignorert.
     *
     * @param rad   ei rad som inneheld feltet som datoverdien skal hentast frå
     * @param index kolonneindeksen til feltet som inneheld datoverdien
     * @return ein datoverdi henta frå det aktuelle feltet, eventuelt ingenting dersom feltet er tomt eller kun
     * inneheld whitespace
     */
    Optional<LocalDate> readDato(final List<String> rad, final int index) {
        return tilDato(read(rad, index));
    }

    public Optional<LocalDate> tilDato(Optional<String> read) {
        return read
                .map(OversetterSupport::fjernTidFraDato)
                .map(OversetterSupport::tilpassTilDatoFormat)
                .map(Datoar::dato);
    }

    /**
     * Tilpassar dato-verdiar på yyyy-MM-dd-format til dato-formatet yyyy.MM.dd ved å
     * erstatte alle skilletegna.
     *
     * @param text datoverdien som skal konverterast til forventa datoformat
     * @return den nye datoverdien med bindestrek erstatta av punktum
     */
    private static String tilpassTilDatoFormat(final String text) {
        return text.replaceAll("-", ".");
    }

    /**
     * Genererer ein ny dato-verdi der eventuelle tekst frå og med tegn 11, er fjerna frå <code>text</code> viss
     * den er lengre enn 10 tegn.
     *
     * @param text ein datoverdi som skal avkortast til 10-tegn
     * @return ein ny streng som inneheld dei 10 første tegna frå <code>text</code>
     */
    private static String fjernTidFraDato(final String text) {
        return text.length() > 10 ? text.substring(0, 10) : text;
    }

    /**
     * Hentar ut den tekstlige verdien frå den angitte indeksen. Dersom verdien er <code>null</code> eller
     * inneheld kun whitespace, eventuelt er heilt tom, blir ein {@link java.util.Optional#empty() tom} verdi returnert.
     *
     * @param rad   rada som verdien skal hentast frå
     * @param index indexen som peikar til feltet som verdien skal hentast frå
     * @return den tekstlige verdien av feltet på den angitte indeksen i rada, eller ingenting dersom feltets verdi
     * er <code>null</code>, eller dersom det kun inneheld whitespace eller verdien er ein tom tekst-streng
     */
    Optional<String> read(final List<String> rad, final int index) {
        return ofNullable(rad.get(index)).map(String::trim).filter(t -> !t.isEmpty());
    }
}
