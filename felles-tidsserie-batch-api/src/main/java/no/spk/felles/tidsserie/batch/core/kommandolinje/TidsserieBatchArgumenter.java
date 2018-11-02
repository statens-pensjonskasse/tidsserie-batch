package no.spk.felles.tidsserie.batch.core.kommandolinje;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Function;

import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;
import no.spk.felles.tidsserie.batch.core.UttrekksId;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link TidsserieBatchArgumenter} representerer kommandolinjeargumenta som applikasjonar som overstyrer
 * felles-tidsserie-batch må støtte.
 *
 * @since 1.1.0
 */
public interface TidsserieBatchArgumenter {
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

    AntallProsessorar antallProsessorar();

    AldersgrenseForSlettingAvLogKatalogar slettegrense();

    Duration maksimalKjøretid();

    LocalTime avsluttFørTidspunkt();

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
     * enn det felles-tidsserie-batch tilbyr out-of-the-box, kan registrere tjenester som gir tilgang til verdiane
     * av dei applikasjonsspesifikke argumenta via denne metoda.
     *
     * @param registry tjenesteregisteret som argumentspesifikke tjenester kan registrerast i
     * @since 1.1.0
     */
    default void registrer(final ServiceRegistry registry) {
    }
}