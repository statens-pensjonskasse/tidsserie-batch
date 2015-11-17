package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning;

import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.avregning.Premiebeloep;
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
 * St�tteklasse for spesifisering av nye kolonnebaserte tidsserieformat.
 * <br>
 * Spesifikasjonar av nye format m� arve fr� denne klassa og implementere {@link #definer()} for � definere
 * opp kva kolonne som formatet st�ttar.
 * <br>
 * Kvar kolonne blir spesifisert ved � kalle {@link #kolonne(int, String, KolonneMapper)}. Kolonner kan og
 * markerast som obligatoriske ved � kalle {@link KolonneSpesifikasjon#obligatorisk()} p� kolonnespesifikasjonen
 * returnert fr� {@link #kolonne(int, String, KolonneMapper)}.
 * <br>
 * Spesifikasjonen handhevar f�lgjande konsistensreglar:
 * <ul>
 * <li>Det m� eksistere minst eit kolonne</li>
 * <li>Det kan kun eksistere ei kolonne pr kolonnenummer</li>
 * <li>Det kan ikkje eksistere gap mellom kolonnenummera i spesifikasjonen, om det blir lagt inn 4 kolonner m� dei ha nummer 1 til 4 uten gap mellom kolonnene.</li>
 * </ul>
 * <br>
 *
 * @since 1.2.0
 */
abstract class FormatSpesifikasjon {
    protected static final Object ANTALL_FEIL_PLACEHOLDER = new Object();

    private final List<KolonneSpesifikasjon> kolonner = new ArrayList<>();

    private final Desimaltallformatering desimaltall;

    protected FormatSpesifikasjon(final Desimaltallformatering desimaltall) {
        this.desimaltall = desimaltall;
        definer();
        kolonner.sort(KolonneSpesifikasjon::compare);

        if (kolonner.isEmpty()) {
            throw new IllegalArgumentException(
                    "Spesifikasjonen m� inneholde minst ei kolonne"
            );
        }
        if (kolonnenummer().min(Integer::compare).get() > 1) {
            throw new IllegalArgumentException(
                    "Spesifikasjonen manglar ein definisjon for kolonnenummer 1"
            );
        }
        kolonnenummer()
                .collect(groupingBy(Function.<Integer>identity()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> {
                    throw new IllegalArgumentException(
                            "Spesifikasjonen kan ikkje inneholde fleire kolonner med samme kolonnenummer: "
                                    + e.getValue()
                    );
                });
        kolonnenummer().reduce((a, b) -> {
            if (b - a > 1) {
                throw new IllegalArgumentException(
                        "Spesifikasjonen m� inneholde ein definisjon for alle kolonnenummer mellom kolonne 1"
                                + " og kolonne "
                                + kolonnenummer().max(Integer::compare).get()
                                + ".\nDet vart oppdaga eit gap p� "
                                + (b - a - 1)
                                + " kolonne(r) mellom kolonne "
                                + a
                                + " og "
                                + b
                );
            }
            return b;
        });

    }

    protected abstract void definer();

    /**
     * Navnet for alle kolonnene som er definert i formatet.
     *
     * @return alle kolonnenavna som er definert opp, sortert i henhold til kolonnenummer, med kolonne 1 sitt navn f�rst
     */
    public Stream<String> kolonnenavn() {
        return kolonner
                .stream()
                .map(KolonneSpesifikasjon::name);
    }

    /**
     * Genererer ein straum av serialiserte verdiar utleda fr� underlaget og underlagsperioda, for kvar kolonne
     * definert i spesifikasjonen.
     *
     * @param u  observasjonsunderlaget som underlagperioda tilh�yrer
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
     * Spesifiserer kva kolonna p� posisjon {@code kolonneNummer} heiter og korleis rader i formatet skal populere
     * kolonna med verdiar.
     *
     * @param kolonneNummer tall som indikerer kvar kolonna skal plasserast i output-formatet, startar p� kolonne nr 1
     * @param name          navnet p� kolonna, blir typisk brukt i headerrada for � indikere kva kolonna heiter
     * @param mapper        mappingstrategi for � populere kolonna med verdiar
     * @return kolonnespesifikasjonen, kan brukast for � indikere at kolonna er {@link KolonneSpesifikasjon#obligatorisk()}
     */
    protected KolonneSpesifikasjon kolonne(int kolonneNummer, String name, KolonneMapper mapper) {
        final KolonneSpesifikasjon kolonne = new KolonneSpesifikasjon(kolonneNummer, name, mapper);
        kolonner.add(kolonne);
        return kolonne;
    }

    protected String prosent(final Optional<Prosent> verdi, final int antallDesimaler) {
        return verdi.map(p -> prosent(p, antallDesimaler)).orElse("");
    }

    protected String prosent(final Prosent verdi, final int antallDesimaler) {
        return desimaltall.formater(verdi.toDouble() * 100d, antallDesimaler);
    }

    protected String premie(final Premiebeloep beloep) {
        return beloep.toString(desimaltall.desimalFormat(2));
    }

    protected static Optional<Avtale> avtale(final Underlagsperiode up) {
        return up.valgfriAnnotasjonFor(Avtale.class);
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

    protected static String dato(final LocalDate verdi) {
        return verdi.toString();
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

    protected static Object antallFeil() {
        return ANTALL_FEIL_PLACEHOLDER;
    }

    private Stream<Integer> kolonnenummer() {
        return kolonner.stream().map(KolonneSpesifikasjon::nummer);
    }

    /**
     * {@link ErrorDetector} fangar feil og held oversikt over kor mange feil som har oppst�tt ved
     * serialisering av ei enkelt {@link Underlagsperiode}.
     * <br>
     * I etterkant av at underlagsperioda er ferdig serialisert, kan antall feil hentast ut og leggast ved den serialiserte
     * representasjonen av rada for � markere kva rader det er d�rlig datakvalitet p� og ikkje.
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
     * verdiar for kolonna fr� eit observasjonsunderlag eller ei underlagsperiode.
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

        private int nummer() {
            return kolonneNummer;
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
     * {@link KolonneMapper} representerer mappingstrategien for � populere ei kolonne definert via
     * {@link FormatSpesifikasjon#kolonne(int, String, KolonneMapper)} med data henta fr� eit underlag eller
     * ei underlagsperiode.
     *
     * @since 1.2.0
     */
    interface KolonneMapper extends BiFunction<Underlag, Underlagsperiode, Object> {
    }
}
