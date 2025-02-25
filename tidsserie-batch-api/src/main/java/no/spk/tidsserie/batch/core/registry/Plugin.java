package no.spk.tidsserie.batch.core.registry;

import static java.lang.String.format;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.ServiceLoader;

import no.spk.tidsserie.batch.core.Katalog;
import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.Tidsseriemodus;
import no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link Plugin} er ein {@link Extensionpoint utvidelse} som gir modular muligheita til å
 * {@link #aktiver(ServiceRegistry) registrere} ekstra tenester i tenesteregisteret under oppstart av
 * tidsserie-batch.
 * <h2>Når i oppstarten blir {@link Plugin#aktiver(ServiceRegistry)} kalla?</h2>
 * Batchen kallar alle plugin si {@link #aktiver(ServiceRegistry)}-metode rett før modusen får muligheita
 * til å {@link Tidsseriemodus#registerServices(ServiceRegistry) registrere} modus-spesifikke tenester under
 * oppstarten av batchen.
 * <h2>Korleis blir plugins gjort tilgjengelig for batchen?</h2>
 * <p>
 * {@link Plugin} sitt hovedformål er å dynamisk utvide tidsserie-batch sin funksjonalitet, uten at batchen sjølv
 * kjenner til eller er avhengig av funksjonaliteten som blir plugga inn. Målet er altså å kunne kompilere og bruke
 * batchens API og applikasjonsmodul uten å få med alle avhengigheiter som eventuelle valfrie plugins kan måtte ha
 * behov for, dersom ein ikkje har tenkt å anvende seg av desse plugina sin funksjonalitet.
 * <p>
 * I den aller første del av oppstarten, før nokon kommandolinjeargument har blitt parsa, lastar batchen automatisk inn
 * alle plugins som ligg tilgjengelige på classpathen via {@link ServiceLoader#load(Class) ServiceLoader APIen}.
 * <p>
 * Kvart plugin som er tilgjengelig vil automatisk bli registrert i {@link ServiceRegistry tenesteregisteret} helt i
 * starten av oppstartsfasen.
 * <p>
 * Kvart plugin som skal bli brukt av batchen må ligge registrert i tenesteregisteret før kommandolinjeargument blir parsa
 * og hoveddelen av oppstarten blir køyrt.
 * <p>
 * Det betyr blant anna at det ikkje vil vere mulig å registrere nokon plugin via
 * {@link Tidsseriemodus#registerServices(ServiceRegistry) modusen} ettersom den først blir kalla etter at
 * plugina har blitt {@link #aktiver(ServiceRegistry) initialisert}.
 * <h2>Avgrensingar</h2>
 * <p>
 * Ettersom plugina blir instansiert før batchen parsar kommandolinjeargumenta for køyringa, kan implementasjonar av
 * {@link Plugin} ikke benytte seg av batchens loggrammeverk, slf4j. Tilsvarande er det sterkt anbefalt å ikkje ha
 * nokon felt eller logikk i konstruktørar som kan feile. Den einaste staden pluginet blr kunne feile, er ved kall til
 * {@link #aktiver(ServiceRegistry)}-metoda.
 *
 * @since 1.1.0
 */
public interface Plugin {
    /**
     * Notifiserer pluginet om at batchen er i ferd med å starte opp og ekstra tenester som skal kunne bli benytta
     * i hovedfasen av oppstarten no kan leggast til i tenesteregisteret.
     * <p>
     * Når pluginet mottar denne callbacken er batchen i følgande tilstand:
     * <ul>
     * <li>Alle kommandolinjeargument har blitt parsa, og er tilgjengelig via tenesteregisteret</li>
     * <li>Loggrammeverk er initialisert og klart for å bli brukt</li>
     * <li>Sletting av gamalt innhold i ut- og log-katalogane har enda ikkje blitt køyrt</li>
     * </ul>
     * <h2>Kva tenester er tilgjengelig for oppslag?</h2>
     * På tidspunktet pluginet blir kalla er det er mulig å slå opp eit avgrensa sett med tenester frå <code>registry</code>:
     * <ul>
     * <li>{@link Path}: Inn-, ut- og log-katalogane til batchen</li>
     * <li>{@link TidsserieBatchArgumenter}: Kommandolinjeargumenta som kjerna av batchen sjølv benyttar seg av</li>
     * <li>{@link AntallProsessorar}: Antall CPU-kjerner som brukaren vil at batchen skal benytte for tidsseriegenerering</li>
     * <li>{@link Tidsseriemodus}: Modusen brukaren har angitt at batchen skal bruke for å generere tidsseriar</li>
     * <li>Andre {@link TidsserieBatchArgumenter#registrer(ServiceRegistry) kommandolinjeargument} som er spesifikke for
     * applikasjonen som tidsserie-batch inngår som ein del av</li>
     * </ul>
     * Denne delen av eit plugin kan ikkje slå opp/vere avhengig av tenester som har blitt/vil bli registrert av andre
     * plugins, rekkefølga plugina blir initialisert i er uspesifisert og kan for alle praktiske formål sjåast på som
     * tilfeldig. Rekkefølga kan variere frå køyring til køyring av batchen.
     * <p>
     * Dersom pluginet har behov for å kalle tenester som kan ha blitt registrert av andre plugins, bør dette først
     * skje i ein seinare fase, via andre typer utvidelsar, for eksempel
     * {@link TidsserieLivssyklus#start(ServiceRegistry)}.
     *
     * @param registry tenesteregisteret som batchen anvendar seg av
     * @see Katalog
     * @see TidsserieBatchArgumenter
     * @see AntallProsessorar
     * @see Tidsseriemodus
     */
    void aktiver(final ServiceRegistry registry);

    /**
     * Registrerer alle plugins som er tilgjengelige via <code>plugins</code>, i
     * {@link ServiceRegistry tenesteregisteret} til batchen.
     * <p>
     * {@link #aktiver(ServiceRegistry) Aktivering} av plugina blir ikkje utført av denne metoda, batchen handterer
     * det seinare i oppstartsprosessen.
     * <p>
     * Typisk vil <code>plugins</code> vere ei samling som er slått opp via
     * {@link ServiceLoader#load(Class) ServiceLoader APIen}.
     *
     * @param registry tenesteregisteret som gjeldande køyring av batchen benyttar seg av
     * @param plugins alle plugins
     * @see ServiceLoader#load(Class)
     */
    static void registrerAlle(final ServiceRegistry registry, final Iterable<Plugin> plugins) {
        final Iterator<Plugin> i = plugins.iterator();

        //noinspection WhileLoopReplaceableByForEach
        while (i.hasNext()) {
            try {
                final Plugin plugin = i.next();
                registry.registerService(Plugin.class, plugin);
            } catch (final RuntimeException | Error e) {
                registry.registerService(Plugin.class, new Plugin() {
                    @Override
                    public void aktiver(final ServiceRegistry ignore) {
                        throw e;
                    }

                    @Override
                    public String toString() {
                        return format(
                                "Feilande plugin, feilmelding ved instansiering: '%s'", e.getMessage()
                        );
                    }
                });
            }
        }
    }
}