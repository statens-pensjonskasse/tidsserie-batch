package no.spk.felles.tidsserie.batch.core.kommandolinje;

import static no.spk.felles.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar.aldersgrenseForSlettingAvLogKatalogar;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Function;

import no.spk.faktura.input.DurationUtil;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.UttrekksId;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link TidsserieBatchArgumenter} representerer kommandolinjeargumenta som applikasjonar som overstyrer
 * tidsserie-batch må støtte.
 * <p>
 * Modusar / applikasjonar som har behov for fleire argument står fritt til å støtte fleire argument.
 * Dei står og fritt til å støtte færre så lenge dei gjennom utledning eller hardkoding, likevel er i stand
 * til å returnere gyldige verdiar for alle argumenta som grensesnittet angir at dei må støtte.
 *
 * @since 1.1.0
 */
public interface TidsserieBatchArgumenter{
    /**
     * Oppsummerer kva argument batchen blir køyrt med, og eventuelle advarslar som brukaren kan ha interesse av å sjå
     * i UIet ved køyring av batchen.
     * <p>
     * Oppsummeringa vil bli vist til brukaren via UIet under oppstart av batchen.
     *
     * @return ei melding som oppsummerer argumenta og advarslar om gyldige, men potensielt uønska kombinasjonar av argument
     */
    @Override
    String toString();

    /**
     * Katalogen som inneheld alle CSV-filene batchen skal lese inn grunnlagsdata frå.
     * <p>
     * Merk at dette representerer ein konkret uttrekkskatalog, ikkje ein inn-katalog som batchen kan søke fram nyaste
     * uttrekkskatalog frå.
     *
     * @return stien til uttrekkskatalogen batchen skal lese grunnlagsdata frå
     */
    Path uttrekkskatalog();

    /**
     * Katalogen som batchen skal generere nye logkatalogar under.
     * <p>
     * Det vil bli generert ein ny underkatalog i denne katalogen pr køyring av batchen.
     *
     * @return katalogen som batchen vil opprette ein ny underkatalog med logfiler i for kvar køyring av batchen
     */
    Path logkatalog();

    /**
     * Katalogen som batchen vil skrive alle produserte filer (utanom logfiler) til.
     * <p>
     * Kvar køyring av batchen vil slette alt innhold i denne katalogen for å sikre at det ikkje akkumulerer seg opp
     * resultat som fyller disken ved mange køyringar av batchen over tid.
     *
     * @return ut-katalogen som batchen lagrar resultatet av køyringa under
     */
    Path utkatalog();

    /**
     * Antall CPU-kjerner batchen maksimalt skal benytte ved generering av tidsseriar.
     * <p>
     * Dette tallet bør typisk ikkje overstige antall tilgjengelige CPU-kjerner på maskina batchen køyrer på,
     * forutsatt at ein følger modellen med å ikkje utføre nokon I/O på trådane som genererer tidsseriar.
     * <p>
     * Returnerer et standard antall prosessorer basert på maskinenes hardware, dersom metoden er uimplementert.
     *
     * @return antall CPU-kjerner batchen skal benytte ved genereing av tidsseriar
     */
    default AntallProsessorar antallProsessorar() {
        return AntallProsessorar.standardAntallProsessorar();
    }

    /**
     * Aldersgrense som regulerer kva loggkatalogar som skal bli automatisk sletta i
     * oppryddingsfasen av oppstarten.
     * <p>
     * Har en default-verdi på 0 hvis uimplementert.
     *
     * @return aldersgrense for sletting av loggkatalogar
     */
    default AldersgrenseForSlettingAvLogKatalogar slettegrense() {
        return aldersgrenseForSlettingAvLogKatalogar(0);
    }

    /**
     * Maksimal køyretid frå batchen startar til den seinast må ha avslutta køyringa.
     * <p>
     * Dersom batchen ikkje har køyrt ferdig innan denne tidsfristen, vil køyringa automatisk bli terminert
     * og køyringa blir avslutta med ei feilkode som indikerer at køyringa tima ut.
     * <p>
     * Dersom dei to måtane for å avgrense køyretida endar opp med å ha forskjellige meiningar om kva som
     * skal vere køyretidspunktet batchen må avsluttast innan, vil det lavaste tidspunktet bli brukt.
     * <p>
     * Har en default-verdi på 24 timer hvis uimplementert.
     *
     * @return maksimal køyretid for batchkøyringa
     * @see #avsluttFørTidspunkt()
     */
    default Duration maksimalKjøretid() {
        //noinspection OptionalGetWithoutIsPresent
        return DurationUtil.convert("2400").get();
    }

    /**
     * Siste køyretidspunkt, batchen må ha avslutta køyringa innan dette tidspunktet.
     * <p>
     * Dersom batchen ikkje har køyrt ferdig innan denne tidsfristen, vil køyringa automatisk bli terminert
     * og køyringa blir avslutta med ei feilkode som indikerer at køyringa tima ut.
     * <p>
     * Dersom dei to måtane for å avgrense køyretida endar opp med å ha forskjellige meiningar om kva som
     * skal vere køyretidspunktet batchen må avsluttast innan, vil det lavaste tidspunktet bli brukt.
     * <p>
     * Har en default-verdi på 23:59 hvis uimplementert.
     *
     * @return tidspunktet batchkøyringa må ha avslutta innan
     * @see #maksimalKjøretid()
     */
    default LocalTime avsluttFørTidspunkt() {
        return LocalTime.parse("23:59");
    }

    /**
     * {@link Tidsseriemodus Modusen} som implementerer den funksjonelle oppførselen til batchen.
     *
     * @return modusen som køyringa skal benytte
     * @see Tidsseriemodus#lagTidsserie(ServiceRegistry)
     */
    Tidsseriemodus modus();

    /**
     * Dersom brukaren ikkje har angitt ein {@link #uttrekkskatalog()}, køyrer den angitte funksjonen med
     * batchen sin inn-katalog som input for å la funksjonen automatisk velge kva uttrekk som automatisk
     * skal brukast som {@link #uttrekkskatalog()}.
     *
     * @param utvelger utvalgsfunksjon, gitt ein inn-katalog, velger kva underkatalog som skal brukast som uttrekkskatalog for batchen
     * @since 1.1.0
     */
    void velgUttrekkVissIkkeAngitt(final Function<Path, UttrekksId> utvelger);

    /**
     * Callback for registrering av applikasjonsspesifikke tjenester som gir tilgang til argument som ikkje inngår
     * som ein del av den generelle APIen for kommandolinjeargument.
     * <p>
     * Applikasjonar som overstyrer parsinga av kommandolinjeargument for å kunne tilby fleire typer innstillingar
     * enn det tidsserie-batch tilbyr out-of-the-box, kan registrere tjenester som gir tilgang til verdiane
     * av dei applikasjonsspesifikke argumenta via denne metoda.
     *
     * @param registry tjenesteregisteret som argumentspesifikke tjenester kan registrerast i
     * @since 1.1.0
     */
    default void registrer(final ServiceRegistry registry) {
    }
}