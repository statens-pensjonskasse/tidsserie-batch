package no.spk.tidsserie.batch.main;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.tidsserie.batch.core.registry.Plugin;
import no.spk.tidsserie.batch.main.spi.ExitCommand;
import no.spk.tidsserie.tjenesteregister.ServiceRegistry;

/**
 * {@link TidsserieMain} er oppstartsklassa for tidsserie-batch.
 * <p>
 * Klassa er ansvarlig for å instansiere det minimum av ytre tenester som krevest for å boostrappe
 * {@link TidsserieBatch platformrammeverket} og kunne {@link TidsserieBatch#run(Supplier, String...) starte opp}
 * batchen.
 * <p>
 * Tenester som main-klassa er ansvarlig for å bootstrappe:
 * <ul>
 * <li>{@link ServiceRegistry Tenesteregisteret}</li>
 * <li>{@link ExitCommand}</li>
 * <li>{@link ApplicationController}</li>
 * <li>{@link View}</li>
 * <li>{@link TidsserieBatchArgumenterParser}</li>
 * </ul>
 * <p>
 * Ingen av tenestene som blir lasta eller metodekalla som blir utført kan benytte seg av nokon form for
 * logrammeverk, dette blir først tilgjengelig for bruk på eit seinare tidspunkt i
 * {@link TidsserieBatch#run(Supplier, String...)}, etter initialisering av batchen sin loggkonfigurasjon.
 */
public class TidsserieMain {
    public static void main(final String... args) {
        try {
            final ServiceRegistry registry = finnFørsteTjeneste(ServiceRegistry.class);
            registry.registerService(View.class, new ConsoleView());

            Plugin.registrerAlle(registry, ServiceLoader.load(Plugin.class));

            new TidsserieBatch(
                    registry,
                    System::exit,
                    new ApplicationController(registry)
            )
                    .run(
                            () -> finnFørsteTjeneste(TidsserieBatchArgumenterParser.class),
                            args
                    );
        } catch (final ManglandeServiceLoaderOppsettError e) {
            System.err.println(e.getMessage());
            System.exit(255);
        }
    }

    static <T> T finnFørsteTjeneste(final Class<T> type) {
        final Iterator<T> i = ServiceLoader.load(type).iterator();
        if (i.hasNext()) {
            return i.next();
        }
        throw new ManglandeServiceLoaderOppsettError(type);
    }
}
