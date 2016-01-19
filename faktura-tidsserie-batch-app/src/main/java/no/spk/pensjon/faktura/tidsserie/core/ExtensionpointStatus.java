package no.spk.pensjon.faktura.tidsserie.core;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Status som inneheld resultatet frå {@link Extensionpoint#invokeAll(Consumer)}.
 * <br>
 * Ein status tilbyr muligheit for klientane av eit extension point til å bestemme korleis feil frå eit kall til
 * meir enn ei tjeneste av samme type, skal handterast dersom ei eller fleire av tjenestene kasta ein exception
 * når dei vart kalla.
 * <br>
 * Tanken er at nokon klientar kjem til å ønskjer å ignorere feilen heilt, nokon logge men ellers ignorere den medan
 * nokon andre ønskjer å feile sjølv når tjenester dei kallar feilar.
 *
 * @author Tarjei Skorgenes
 * @since 2.1.0
 */
public class ExtensionpointStatus {
    private final List<Exception> results = new ArrayList<>();

    static ExtensionpointStatus ok() {
        return new ExtensionpointStatus(Stream.empty());
    }

    private ExtensionpointStatus(final Stream<Exception> results) {
        results.forEach(this.results::add);
    }

    static ExtensionpointStatus merge(final ExtensionpointStatus a, final ExtensionpointStatus b) {
        return new ExtensionpointStatus(
                Stream.concat(
                        a.results.stream(),
                        b.results.stream()
                )
        );
    }

    <T> ExtensionpointStatus invoke(final Consumer<T> operation, T extension) {
        try {
            operation.accept(extension);
        } catch (final Exception t) {
            results.add(t);
        }
        return this;
    }

    /**
     * Indikerer korvidt {@link Extensionpoint#invokeAll(Consumer)} har feila eller ikkje.
     *
     * @return {@code true} dersom kallet har feila, {@code false} ellers
     */
    public boolean hasFailed() {
        return !results.isEmpty();
    }

    /**
     * Returnerer ein straum med alle feil som statusen inneheld.
     * <br>
     * Dersom {@link #hasFailed()} er {@code false} er straumen tom, i alle andre situasjonar skal den inneholde
     * minst ein feil.
     *
     * @return ein straum med alle feil som vart fanga av {@link Extensionpoint#invokeAll(Consumer)}
     */
    public Stream<Exception> stream() {
        return results.stream();
    }

    /**
     * Itererer over alle feil som statusen inneheld.
     *
     * @param failure callbacken som får tilsendt kvar av feila statusen inneheld
     */
    public void forEach(final Consumer<Exception> failure) {
        results.forEach(failure);
    }

    /**
     * Kastar feilen generert av {@code creator} dersom statusen {@link #hasFailed() har feila}.
     * <br>
     * Dersom statusen er ok og ikkje inneheld nokon feil returnerer metoda utan å kaste ein exception.
     *
     * @param creator ein funksjon som tar inn lista med alle feil som oppstod under kallet til
     *                {@link Extensionpoint#invokeAll(Consumer)} og returnerer ein exception som
     *                beskriv kva som har feila
     * @param <X>     feiltypen som skal bli kasta
     * @throws X dersom {@link #hasFailed} indikerer at kallet har feila
     */
    public <X extends Throwable> void orElseThrow(final Function<Stream<Exception>, X> creator) throws X {
        if (hasFailed()) {
            throw creator.apply(results.stream());
        }
    }

    @Override
    public String toString() {
        return !hasFailed() ?
                "OK"
                :
                "Feila\nResultat:\n"
                        + results
                        .stream()
                        .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
                        .map(line -> "\t- " + line)
                        .collect(joining("\n"));
    }
}
