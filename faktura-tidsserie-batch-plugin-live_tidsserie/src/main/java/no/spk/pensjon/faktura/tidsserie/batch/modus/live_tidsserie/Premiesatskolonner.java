package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.beloep;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.flagg;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.kode;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link Premiesatskolonner} representerer uthentings- og formateringslogikken
 * som skal anvendast ved uthenting av 5 kolonner pr produkt som ein tar med i CSV-formatet.
 * <br>
 * Premiesatsar angitt i prosent blir formatert med 2 desimalar, premiesatsar angitt som kronebeløp
 * blir angitt utan desimalar.
 *
 * @author Tarjei Skorgenes
 */
class Premiesatskolonner {
    private static final EnumSet<Produkt> PENSJONSPRODUKT = EnumSet.of(Produkt.PEN, Produkt.AFP, Produkt.TIP);
    private static final EnumSet<Produkt> YSK_GRU_PRODUKT = EnumSet.of(Produkt.YSK, Produkt.GRU);

    /**
     * Returnerer ein straum av funksjonar som trekker ut verdiar for dei 5 kolonnene
     * som beskriv premiesatsinformasjon for det angitte produktet.
     * <br>
     * Første funksjon  trekker ut informasjon om korvidt produktet er fakturerbart eller ei.
     * <br>
     * Andre, tredje og fjerde funksjon trekker ut gjeldande premiesatsar for produket, enten som prosent
     * eller som kronebeløp avhengig av typen produkt.
     * <br>
     * Femte og siste kolonne inneheld produktinfo-koda for produktet.
     * <br>
     * Dersom avtalen ikkje har produktet som blir forsøkt slått opp, blir det returnert 5 funksjonar som
     * genererer tomme-verdiar.
     *
     * @param periode
     * @param premiesats produktet som premiesats-funksjonar skal hentast ut for
     * @return ein straum med 5 funksjonar som trekker ut premiesats-informasjon frå ei underlagsperiode
     */
    Stream<Function<Underlagsperiode, String>> forPremiesats(final Underlagsperiode periode, final Optional<Premiesats> premiesats) {
        return premiesats
                .filter(s -> erPensjonsprodukt(s) || erYskEllerGru(s))
                .map(s -> premiesatsverdier(s, periode))
                .orElse(Stream.of(
                        empty(),
                        empty(),
                        empty(),
                        empty(),
                        empty()
                ));
    }

    private Stream<Function<Underlagsperiode, String>> premiesatsverdier(Premiesats premiesats, Underlagsperiode periode) {
        if (erPensjonsprodukt(premiesats)) {
            return premiesatsCache.computeIfAbsent(premiesats,
                    key -> asList(
                            new Memento(erFakturerbar(premiesats).apply(periode)),
                            new Memento(arbeidsgiverprosent(premiesats).apply(periode)),
                            new Memento(medlemsprosent(premiesats).apply(periode)),
                            new Memento(administrasjonsgebyrprosent(premiesats).apply(periode)),
                            new Memento(produktinfo(premiesats).apply(periode))
                    )
            ).stream();
        } else if (erYskEllerGru(premiesats)) {
            return premiesatsCache.computeIfAbsent(premiesats,
                    key -> asList(
                            new Memento(erFakturerbar(premiesats).apply(periode)),
                            new Memento(arbeidsgiverbeloep(premiesats).apply(periode)),
                            new Memento(medlemsbeloep(premiesats).apply(periode)),
                            new Memento(administrasjonsgebyrbeloep(premiesats).apply(periode)),
                            new Memento(produktinfo(premiesats).apply(periode))
                    )
            ).stream();
        }

        throw new IllegalArgumentException("Denne metoden kan kun benyttes for premiesatser som tilhører PEN, AFP, TIP, YSK og GRU.");
    }

    private boolean erYskEllerGru(Premiesats premiesats) {
        return YSK_GRU_PRODUKT.contains(premiesats.produkt);
    }

    private boolean erPensjonsprodukt(Premiesats premiesats) {
        return PENSJONSPRODUKT.contains(premiesats.produkt);
    }

    private final ConcurrentMap<Premiesats, List<Function<Underlagsperiode, String>>> premiesatsCache = new ConcurrentHashMap<>(200_000);

    private static class Memento implements Function<Underlagsperiode, String> {
        private final String value;

        public Memento(final String value) {
            this.value = value;
        }

        @Override
        public String apply(final Underlagsperiode o) {
            return value;
        }
    }

    private final Desimaltallformatering desimalar = new Desimaltallformatering();

    private Function<Underlagsperiode, String> erFakturerbar(final Premiesats premiesats) {
        return up -> flagg(premiesats.erFakturerbar());
    }

    private Function<Underlagsperiode, String> arbeidsgiverbeloep(final Premiesats premiesats) {
        return up -> beloepsatser(premiesats, Satser::arbeidsgiverpremie);
    }

    private Function<Underlagsperiode, String> medlemsbeloep(final Premiesats premiesats) {
        return up -> beloepsatser(premiesats, Satser::medlemspremie);
    }

    private Function<Underlagsperiode, String> administrasjonsgebyrbeloep(final Premiesats premiesats) {
        return up -> beloepsatser(premiesats, Satser::administrasjonsgebyr);
    }

    private Function<Underlagsperiode, String> arbeidsgiverprosent(final Premiesats premiesats) {
        return up -> prosentsatser(premiesats, Satser::arbeidsgiverpremie);
    }

    private Function<Underlagsperiode, String> medlemsprosent(final Premiesats premiesats) {
        return up -> prosentsatser(premiesats, Satser::medlemspremie);
    }

    private Function<Underlagsperiode, String> administrasjonsgebyrprosent(final Premiesats premiesats) {
        return up -> prosentsatser(premiesats, Satser::administrasjonsgebyr);
    }

    private Function<Underlagsperiode, String> produktinfo(final Premiesats premiesats) {
        return up -> kode(premiesats.produktinfo.toString());
    }

    private Function<Underlagsperiode, String> empty() {
        return up -> "";
    }

    private String beloepsatser(Premiesats premiesats, Function<Satser<Kroner>, Kroner> mapper) {
        return beloep(premiesats.beloepsatsar().map(mapper));
    }

    private String prosentsatser(Premiesats premiesats, Function<Satser<Prosent>, Prosent> mapper) {
        return prosent(premiesats.prosentsatser().map(mapper), 2);
    }

    private String prosent(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosent(p, antallDesimaler)).orElse("");
    }

    private String prosent(final Prosent verdi, final int antallDesimaler) {
        return desimalar.formater(verdi.toDouble() * 100d, antallDesimaler);
    }

    private static Optional<Premiesats> premiesats(final Underlagsperiode up, final Produkt produkt) {
        return up.valgfriAnnotasjonFor(Avtale.class).flatMap(a -> a.premiesatsFor(produkt));
    }
}
