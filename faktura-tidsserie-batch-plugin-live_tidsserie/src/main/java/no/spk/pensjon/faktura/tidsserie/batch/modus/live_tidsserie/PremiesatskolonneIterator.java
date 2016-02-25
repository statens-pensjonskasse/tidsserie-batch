package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link PremiesatskolonneIterator} brukes for å iterere gjennom en og en premiesatsverdi-mapperfunksjon hentet fra
 * {@link Premiesatskolonner#forPremiesats(Underlagsperiode, Optional)}, for en konkret underlagsperiode.
 *
 * Klassen eksisterer for å redusere antall oppslag av {@link Avtale} og {@link Premiesats} for et gitt produkt, og for å
 * dra nytte av de cachede verdier i {@link Premiesatskolonner}.
 *
 * For et gitt {@link Produkt} forventes det at {@link #nestePremiesatsverdiFor(Underlagsperiode)} blir kalt like mange ganger som det er
 * premiesatskolonner for produktet i {@link Datavarehusformat}.
 *
 * Når alle verdiene er hentet for en underlagsperiode (pt. 5 verdier), vil et nytt kall ti {@link #nestePremiesatsverdiFor(Underlagsperiode)}
 * føre til at {@link Premiesatskolonner#forPremiesats(Underlagsperiode, Optional)} blir hentet på nytt.
 * Dvs. at det gjøres ett kall til {@link Premiesatskolonner#forPremiesats(Underlagsperiode, Optional)} for hvert sett med premiesatsverdier
 * PremiesatskolonneIterator er å kunne produsere.
 *
 * @author Snorre E. Brekke - Computas
 */
class PremiesatskolonneIterator {
    private final Premiesatskolonner premiesatskolonner;
    private final Produkt produkt;

    private Iterator<Function<Underlagsperiode, String>> cache;
    private Underlagsperiode currentPeriode;

    PremiesatskolonneIterator(Premiesatskolonner premiesatskolonner, Produkt produkt) {
        this.produkt = produkt;
        this.premiesatskolonner = premiesatskolonner;
    }

    /**
     * Denne metoden er ment å kalles én gang for hver premiesatskolonne for et produkt i {@link Datavarehusformat}.
     * Verdiene som returneres er gitt av {@link Premiesatskolonner#forPremiesats(Underlagsperiode, Optional)}
     * @param underlagsperiode perioden som neste premiesatsverdi for produktet denne iteratoren gjelder
     * @return verdien på neste premiesatskolonne for produktet iteratoren gjelder
     * @throws IllegalStateException dersom underlagsperiode-instansen som det blir hentet premiesatsverdier for endres før alle premiesatskolonner er behandlet
     */
    String nestePremiesatsverdiFor(Underlagsperiode underlagsperiode) {
        if (cache == null) {
            Optional<Premiesats> premiesats = underlagsperiode.valgfriAnnotasjonFor(Avtale.class).flatMap(a ->  a.premiesatsFor(produkt));
            cache = premiesatskolonner.forPremiesats(underlagsperiode, premiesats).iterator();
            currentPeriode = underlagsperiode;
        }

        if (underlagsperiode != currentPeriode) {
            throw new IllegalStateException("En ny underlagsperiode ble forsøkt behandlet, før samtlige premiesatskolonner for gjeldende underlagperiode var konsumert.");
        }

        final String verdi = cache.next().apply(underlagsperiode);
        if (!cache.hasNext()) {
            cache = null;
        }
        return verdi;
    }
}
