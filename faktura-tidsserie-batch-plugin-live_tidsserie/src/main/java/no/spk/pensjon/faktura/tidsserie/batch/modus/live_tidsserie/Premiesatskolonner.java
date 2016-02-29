package no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie;

import static java.util.Arrays.asList;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.beloep;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.flagg;
import static no.spk.pensjon.faktura.tidsserie.batch.modus.live_tidsserie.Kolonnetyper.kode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiesats;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Produkt;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

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

    private static final String EMPTY_VALUE = "";
    private static final List<String> EMPTY_VALUES = immutableList(
            EMPTY_VALUE,
            EMPTY_VALUE,
            EMPTY_VALUE,
            EMPTY_VALUE,
            EMPTY_VALUE
    );

    private final ConcurrentMap<Premiesats, List<String>> premiesatsCache = new ConcurrentHashMap<>(200_000);
    private final Desimaltallformatering desimalar = new Desimaltallformatering();

    //Microoptimalisering: Pre-instansierte lambdaer
    private Function<Premiesats, List<String>> prosentsatser;
    private Function<Premiesats, List<String>> beloepsatser;

    public Premiesatskolonner() {
        this.prosentsatser = premiesats -> immutableList(
                erFakturerbar(premiesats),
                arbeidsgiverprosent(premiesats),
                medlemsprosent(premiesats),
                administrasjonsgebyrprosent(premiesats),
                produktinfo(premiesats)
        );
        this.beloepsatser = premiesats -> immutableList(
                erFakturerbar(premiesats),
                arbeidsgiverbeloep(premiesats),
                medlemsbeloep(premiesats),
                administrasjonsgebyrbeloep(premiesats),
                produktinfo(premiesats)
        );
    }

    /**
     * Returnerer ein liste med verdier for dei 5 kolonnene som beskriv premiesatsinformasjon for det angitte produktet.
     * <br>
     * Første angir om produktet er fakturerbart eller ei.
     * <br>
     * Andre, tredje og fjerde verdien angir gjeldande premiesatsar for produket, enten som prosent
     * eller som kronebeløp avhengig av typen produkt.
     * <br>
     * Femte og siste kolonne inneheld produktinfo-koda for produktet.
     * <br>
     * Dersom avtalen ikkje har produktet som blir forsøkt slått opp, blir det returnert 5 tomme verdier.
     * <p>
     *
     * @param premiesats produktet som premiesats-funksjonar skal hentast ut for
     * @return ein liste med 5 verdier som trekker ut premiesats-informasjon frå ei underlagsperiode
     */
    List<String> forPremiesats(final Optional<Premiesats> premiesats) {
        return premiesats
                .map(this::premiesatsverdier)
                .orElse(EMPTY_VALUES);
    }

    private List<String> premiesatsverdier(Premiesats premiesats) {
        final Produkt produkt = premiesats.produkt;
        if (Produkt.YSK == produkt || Produkt.GRU == produkt) {
            return premiesatsCache.computeIfAbsent(premiesats, beloepsatser);
        } else if (Produkt.PEN == produkt || Produkt.AFP == produkt || Produkt.TIP == produkt) {
            return premiesatsCache.computeIfAbsent(premiesats, prosentsatser);
        }
        return EMPTY_VALUES;
    }

    private static List<String> immutableList(String... verdier) {
        return Collections.unmodifiableList(asList(verdier));
    }

    private String erFakturerbar(final Premiesats premiesats) {
        return flagg(premiesats.erFakturerbar());
    }

    private String arbeidsgiverbeloep(final Premiesats premiesats) {
        return beloepsatser(premiesats, Satser::arbeidsgiverpremie);
    }

    private String medlemsbeloep(final Premiesats premiesats) {
        return beloepsatser(premiesats, Satser::medlemspremie);
    }

    private String administrasjonsgebyrbeloep(final Premiesats premiesats) {
        return beloepsatser(premiesats, Satser::administrasjonsgebyr);
    }

    private String arbeidsgiverprosent(final Premiesats premiesats) {
        return prosentsatser(premiesats, Satser::arbeidsgiverpremie);
    }

    private String medlemsprosent(final Premiesats premiesats) {
        return prosentsatser(premiesats, Satser::medlemspremie);
    }

    private String administrasjonsgebyrprosent(final Premiesats premiesats) {
        return prosentsatser(premiesats, Satser::administrasjonsgebyr);
    }

    private String produktinfo(final Premiesats premiesats) {
        return kode(premiesats.produktinfo.toString());
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
}
