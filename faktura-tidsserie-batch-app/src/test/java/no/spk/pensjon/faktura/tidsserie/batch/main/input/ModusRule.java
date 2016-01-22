package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.Tidsseriemodus;

import org.junit.rules.ExternalResource;

/**
 * Metoderegel som tømmer {@link Modus} for modusar innlagt av den nyligast køyrte testen.
 * <br>
 * Merk at sidan {@link Modus} er ein global JVM-singleton, kan ein ikkje køyre testar som benyttar den,
 * i parallell.
 *
 * @since 2.1.0
 */
public class ModusRule extends ExternalResource {
    @Override
    public void after() {
        Modus.reload(Stream.empty());
    }

    /**
     * Populerer {@link Modus} basert på {@link Tidsseriemodus}-mockar for kvart av dei angitte modusnavna.
     *
     * @param modusnavn eit variabelt antall modusnavn som modus skal settast opp til å støtte
     * @see Modus#reload(Stream)
     */
    public void support(final String... modusnavn) {
        Modus.reload(
                asList(modusnavn)
                        .stream()
                        .map(this::create)
        );
    }

    /**
     * Populerer {@link Modus} med alle dei reelle tidsseriemodusane som er tilgjengelig via classpathen til
     * prosjektet.
     *
     * @see Modus#autodetect()
     */
    public void autodetect() {
        Modus.autodetect();
    }

    private Tidsseriemodus create(final String navn) {
        final Tidsseriemodus modus = mock(Tidsseriemodus.class, navn);
        when(modus.navn()).thenReturn(navn);
        return modus;
    }
}
