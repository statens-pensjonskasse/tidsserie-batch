package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder.Kolonnetyper.beloep;
import static no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder.Kolonnetyper.flagg;
import static no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder.Kolonnetyper.kode;

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
     * @param p
     * @param produkt produktet som premiesats-funksjonar skal hentast ut for
     * @return ein straum med 5 funksjonar som trekker ut premiesats-informasjon frå ei underlagsperiode
     */
    Stream<Function<Underlagsperiode, String>> forProdukt(final Underlagsperiode p, final Produkt produkt) {
        Function<Premiesats, Stream<Function<Underlagsperiode, String>>> memento = premiesats -> Stream.empty();
        if (produkt == Produkt.PEN || produkt == Produkt.AFP || produkt == Produkt.TIP) {
            memento = premiesats -> {
                List<Function<Underlagsperiode, String>> functions = premiesatsCache.computeIfAbsent(premiesats,
                        key -> asList(
                                new Memento(erFakturerbar(produkt).apply(p)),
                                new Memento(arbeidsgiverprosent(produkt).apply(p)),
                                new Memento(medlemsprosent(produkt).apply(p)),
                                new Memento(administrasjonsgebyrprosent(produkt).apply(p)),
                                new Memento(produktinfo(produkt).apply(p))
                        )
                );
                return functions.stream();
            };
        } else if (produkt == Produkt.GRU || produkt == Produkt.YSK) {
            memento = premiesats -> {
                List<Function<Underlagsperiode, String>> functions = premiesatsCache.computeIfAbsent(premiesats,
                        key -> asList(
                                new Memento(erFakturerbar(produkt).apply(p)),
                                new Memento(arbeidsgiverbeloep(produkt).apply(p)),
                                new Memento(medlemsbeloep(produkt).apply(p)),
                                new Memento(administrasjonsgebyrbeloep(produkt).apply(p)),
                                new Memento(produktinfo(produkt).apply(p))
                        )
                );
                return functions.stream();
            };
        }
        return p.valgfriAnnotasjonFor(Avtale.class).map(a -> a.premiesatsFor(produkt)).filter(Optional::isPresent).map(Optional::get)
                .map(memento)
                .orElse(Stream.of(
                                empty(),
                                empty(),
                                empty(),
                                empty(),
                                empty()
                        )
                );
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

    private Function<Underlagsperiode, String> erFakturerbar(final Produkt produkt) {
        return up -> flagg(premiesats(up, produkt).map(Premiesats::erFakturerbar));
    }

    private Function<Underlagsperiode, String> arbeidsgiverbeloep(final Produkt produkt) {
        return up -> beloepsatser(up, produkt, Satser::arbeidsgiverpremie);
    }

    private Function<Underlagsperiode, String> medlemsbeloep(final Produkt produkt) {
        return up -> beloepsatser(up, produkt, Satser::medlemspremie);
    }

    private Function<Underlagsperiode, String> administrasjonsgebyrbeloep(final Produkt produkt) {
        return up -> beloepsatser(up, produkt, Satser::administrasjonsgebyr);
    }

    private Function<Underlagsperiode, String> arbeidsgiverprosent(final Produkt produkt) {
        return up -> prosentsatser(up, produkt, Satser::arbeidsgiverpremie);
    }

    private Function<Underlagsperiode, String> medlemsprosent(final Produkt produkt) {
        return up -> prosentsatser(up, produkt, Satser::medlemspremie);
    }

    private Function<Underlagsperiode, String> administrasjonsgebyrprosent(final Produkt produkt) {
        return up -> prosentsatser(up, produkt, Satser::administrasjonsgebyr);
    }

    private Function<Underlagsperiode, String> produktinfo(final Produkt produkt) {
        return up -> kode(premiesats(up, produkt).map(p -> p.produktinfo));
    }

    private Function<Underlagsperiode, String> empty() {
        return up -> "";
    }

    private String beloepsatser(Underlagsperiode up, Produkt produkt, Function<Satser<Kroner>, Kroner> mapper) {
        return beloep(premiesats(up, produkt).flatMap(Premiesats::beloepsatsar).map(mapper));
    }

    private String prosentsatser(Underlagsperiode up, Produkt produkt, Function<Satser<Prosent>, Prosent> mapper) {
        return prosent(premiesats(up, produkt).flatMap(Premiesats::prosentsatser).map(mapper), 2);
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
