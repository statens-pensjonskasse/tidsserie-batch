package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produktinfo;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * Støtteklasse for spesifisering av nye kolonnebaserte tidsserieformat.
 * <br>
 * Spesifikasjonar av nye format må arve frå denne klassa og implementere {@link #definer()} for å definere
 * opp kva kolonne som formatet støttar.
 * <br>
 * Kvar kolonne blir spesifisert ved å kalle {@link #kolonne(String, KolonneMapper)}. Kolonner kan og
 * markerast som obligatoriske ved å kalle {@link KolonneSpesifikasjon#obligatorisk()} på kolonnespesifikasjonen
 * returnert frå {@link #kolonne(String, KolonneMapper)}.
 * <br>
 */
abstract class FormatSpesifikasjon {
    protected static final Object ANTALL_FEIL_PLACEHOLDER = new Object();

    private final List<KolonneSpesifikasjon> kolonner = new ArrayList<>();

    private final Desimaltallformatering desimaltall;

    private int kolonneAntall = 0;

    protected FormatSpesifikasjon(final Desimaltallformatering desimaltall) {
        this.desimaltall = desimaltall;
        definer();
        kolonner.sort(KolonneSpesifikasjon::compare);
    }

    protected abstract void definer();

    /**
     * Navnet for alle kolonnene som er definert i formatet.
     *
     * @return alle kolonnenavna som er definert opp, sortert i henhold til kolonnenummer, med kolonne 1 sitt navn først
     */
    public Stream<String> kolonnenavn() {
        return kolonner
                .stream()
                .map(KolonneSpesifikasjon::name);
    }

    /**
     * Genererer ein straum av serialiserte verdiar utleda frå underlaget og underlagsperioda, for kvar kolonne
     * definert i spesifikasjonen.
     *
     * @param u observasjonsunderlaget som underlagperioda tilhøyrer
     * @param up underlagsperioda som skal serialiserast til ei rad i CSV-fila spesifikasjonen definerer formatet til
     * @return ein straum av serialiserte verdiar for underlagsperioda i henhold til formatspesifikasjonen
     */
    public Stream<Object> serialiser(final Underlag u, final Underlagsperiode up) {
        final ErrorDetector detector = new ErrorDetector();
        return kolonner
                .stream()
                .map(s -> detector.utfoer(s, u, up))
                .map(o -> o == ANTALL_FEIL_PLACEHOLDER ? heiltall(detector.antallFeil) : o);
    }

    /**
     * Spesifiserer kva kolonna på posisjon {@code kolonneNummer} heiter og korleis rader i formatet skal populere
     * kolonna med verdiar.
     *
     * @param name navnet på kolonna, blir typisk brukt i headerrada for å indikere kva kolonna heiter
     * @param mapper mappingstrategi for å populere kolonna med verdiar
     * @return kolonnespesifikasjonen, kan brukast for å indikere at kolonna er {@link KolonneSpesifikasjon#obligatorisk()}
     */
    protected KolonneSpesifikasjon kolonne(String name, KolonneMapper mapper) {
        final KolonneSpesifikasjon kolonne = new KolonneSpesifikasjon(kolonneAntall++, name, mapper);
        kolonner.add(kolonne);
        return kolonne;
    }

    protected String prosent(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosent(p, antallDesimaler)).orElse("");
    }

    protected String prosent(final Prosent verdi, final int antallDesimaler) {
        return desimaltall.formater(verdi.toDouble() * 100d, antallDesimaler);
    }

    protected String prosentSomDesimal(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosentSomDesimal(p, antallDesimaler)).orElse("");
    }

    protected String prosentSomDesimal(final Prosent verdi, final int antallDesimaler) {
        return desimaltall.formater(verdi.toDouble(), antallDesimaler);
    }

    protected static Optional<Avtale> avtale(final Underlagsperiode up) {
        return up.valgfriAnnotasjonFor(Avtale.class);
    }

    protected static String dato(final LocalDate verdi) {
        return verdi.toString();
    }

    protected static String dato(final Optional<LocalDate> verdi) {
        return verdi.map(Object::toString).orElse("");
    }

    protected static String heiltall(final int verdi) {
        return Integer.toString(verdi);
    }

    protected static String heiltall(final long verdi) {
        return Long.toString(verdi);
    }

    protected static String flagg(final boolean value) {
        return value ? "1" : "0";
    }

    protected static String flagg(final Optional<Boolean> value) {
        return value.map(FormatSpesifikasjon::flagg).orElse("");
    }

    protected static String kode(final String verdi) {
        return verdi;
    }

    protected static String kode(final Optional<?> verdi) {
        return verdi.map(Object::toString).orElse("");
    }

    protected static String beloep(final Optional<Kroner> verdi) {
        return verdi.map(FormatSpesifikasjon::beloep).orElse("");
    }

    protected static String beloep(final Kroner beloep) {
        return Long.toString(beloep.verdi());
    }

    protected static Optional<Premiesats> premiesats(final Underlagsperiode up, final Produkt produkt) {
        return avtale(up).flatMap(a -> a.premiesatsFor(produkt));
    }

    protected static Optional<Satser<Kroner>> beloepsatser(final Underlagsperiode up, final Produkt produkt) {
        return premiesats(up, produkt).flatMap(Premiesats::beloepsatsar);
    }

    protected static Optional<Produktinfo> produktinfo(final Underlagsperiode up, final Produkt produkt) {
        return premiesats(up, produkt).map(p -> p.produktinfo);
    }

    protected static Optional<Satser<Prosent>> prosentsatser(final Underlagsperiode up, final Produkt produkt) {
        return premiesats(up, produkt).flatMap(Premiesats::prosentsatser);
    }

    protected static Optional<Boolean> erFakturerbar(final Underlagsperiode up, final Produkt produkt) {
        return premiesats(up, produkt).map(Premiesats::erFakturerbar);
    }

    protected static Optional<Prosent> sumPremiesats(Underlagsperiode p, Produkt produkt) {
        return prosentsatser(p, produkt).map(s -> s.arbeidsgiverpremie().plus(s.medlemspremie()).plus(s.administrasjonsgebyr()));
    }

    protected static Optional<Prosent> sumProsentsatsTotal(Underlagsperiode p) {
        return Stream.of(
                sumPremiesats(p, Produkt.PEN),
                sumPremiesats(p, Produkt.AFP),
                sumPremiesats(p, Produkt.TIP)
        ).filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(Prosent::plus);
    }

    protected static Optional<Kroner> sumPremiesatsBeloep(Underlagsperiode p, Produkt produkt) {
        return beloepsatser(p, produkt).map(s -> s.arbeidsgiverpremie().plus(s.medlemspremie()).plus(s.administrasjonsgebyr()));
    }

    protected static Optional<Prosent> administrasjonsgebyr(Underlagsperiode p, Produkt produkt) {
        return prosentsatser(p, produkt).map(Satser::administrasjonsgebyr);
    }

    protected Optional<Prosent> medlemspremie(Underlagsperiode p, Produkt produkt) {
        return prosentsatser(p, produkt).map(Satser::medlemspremie);
    }

    protected Optional<Prosent> arbeidsgiverpremie(Underlagsperiode p, Produkt produkt) {
        return prosentsatser(p, produkt).map(Satser::arbeidsgiverpremie);
    }

    protected static Optional<Kroner> administrasjonsBeloep(Underlagsperiode p, Produkt produkt) {
        return beloepsatser(p, produkt).map(Satser::administrasjonsgebyr);
    }

    protected Optional<Kroner> medlemspremieBeloep(Underlagsperiode p, Produkt produkt) {
        return beloepsatser(p, produkt).map(Satser::medlemspremie);
    }

    protected Optional<Kroner> arbeidsgiverpremieBeloep(Underlagsperiode p, Produkt produkt) {
        return beloepsatser(p, produkt).map(Satser::arbeidsgiverpremie);
    }

    protected static Object antallFeil() {
        return ANTALL_FEIL_PLACEHOLDER;
    }

    /**
     * {@link ErrorDetector} fangar feil og held oversikt over kor mange feil som har oppstått ved
     * serialisering av ei enkelt {@link Underlagsperiode}.
     * <br>
     * I etterkant av at underlagsperioda er ferdig serialisert, kan antall feil hentast ut og leggast ved den serialiserte
     * representasjonen av rada for å markere kva rader det er dårlig datakvalitet på og ikkje.
     *
     * @since 1.2.0
     */
    private static class ErrorDetector {
        int antallFeil;

        Object utfoer(final KolonneSpesifikasjon kolonne, final Underlag u, final Underlagsperiode up) {
            if (kolonne.erObligatorisk()) {
                return kolonne.map(u, up);
            }
            return ignorerFeil(kolonne, u, up);
        }

        private Object ignorerFeil(KolonneSpesifikasjon mapper, Underlag u, Underlagsperiode up) {
            try {
                return mapper.map(u, up);
            } catch (final Exception e) {
                antallFeil++;
                return "";
            }
        }
    }

    /**
     * {@link KolonneSpesifikasjon} representerer ei kolonne i CSV-formatet med mappingstrategi for korleis hente ut
     * verdiar for kolonna frå eit observasjonsunderlag eller ei underlagsperiode.
     *
     * @since 1.2.0
     */
    static class KolonneSpesifikasjon {
        private final KolonneMapper mapper;
        private final int kolonneNummer;
        private final String name;
        private boolean obligatorisk;

        private KolonneSpesifikasjon(final int kolonneNummer, final String name, final KolonneMapper mapper) {
            this.kolonneNummer = kolonneNummer;
            this.name = name;
            this.mapper = mapper;
        }

        private Object map(final Underlag u, final Underlagsperiode up) {
            return mapper.apply(u, up);
        }

        KolonneSpesifikasjon obligatorisk() {
            obligatorisk = true;
            return this;
        }

        private boolean erObligatorisk() {
            return obligatorisk;
        }

        private String name() {
            return name;
        }

        private static int compare(final KolonneSpesifikasjon a, final KolonneSpesifikasjon b) {
            return Integer.compare(a.kolonneNummer, b.kolonneNummer);
        }

        @Override
        public String toString() {
            return kolonneNummer + "," + name + "," + (obligatorisk ? "obligatorisk" : "valgfri");
        }
    }

    /**
     * {@link KolonneMapper} representerer mappingstrategien for å populere ei kolonne definert via
     * {@link FormatSpesifikasjon#kolonne(int, String, KolonneMapper)} med data henta frå eit underlag eller
     * ei underlagsperiode.
     *
     * @since 1.2.0
     */
    interface KolonneMapper extends BiFunction<Underlag, Underlagsperiode, Object> {
    }
}
