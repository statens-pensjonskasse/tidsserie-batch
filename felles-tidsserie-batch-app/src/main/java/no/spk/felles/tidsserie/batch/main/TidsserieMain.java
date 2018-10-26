package no.spk.felles.tidsserie.batch.main;

import java.util.ServiceLoader;

import no.spk.felles.tidsserie.batch.main.input.Modus;
import no.spk.felles.tidsserie.batch.main.spi.ExitCommand;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link TidsserieMain} er oppstartsklassa for felles-tidsserie-batch.
 * <p>
 * Klassa er ansvarlig for å instansiere det minimum av ytre tenester som krevest for å boostrappe
 * {@link TidsserieBatch platformrammeverket} og kunne {@link TidsserieBatch#run(String...) starte opp}
 * batchen.
 * <p>
 * Tenester som main-klassa er ansvarlig for å bootstrappe:
 * <ul>
 * <li>{@link ServiceRegistry Tenesteregisteret}</li>
 * <li>{@link Modus#autodetect() Støtta modusar}</li>
 * <li>{@link ExitCommand}</li>
 * <li>{@link ApplicationController}</li>
 * <li>{@link View}</li>
 * </ul>
 * <p>
 * Ingen av tenestene som blir lasta eller metodekalla som blir utført kan benytte seg av nokon form for
 * logrammeverk, dette blir først tilgjengelig for bruk på eit seinare tidspunkt i
 * {@link TidsserieBatch#run(String...)}, etter initialisering av batchen sin loggkonfigurasjon.
 */
public class TidsserieMain {
    public static void main(final String... args) {
        Modus.autodetect();

        final ServiceRegistry registry = ServiceLoader.load(ServiceRegistry.class).iterator().next();
        registry.registerService(View.class, new ConsoleView());

        new TidsserieBatch(
                registry,
                System::exit,
                new ApplicationController(registry)
        )
                .run(args);
    }
}
