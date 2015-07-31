package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link ErrorDetector} fangar feil og held oversikt over kor mange feil som har oppstått ved
 * serialisering av ei enkelt {@link Underlagsperiode}.
 * <br>
 * I etterkant av at underlagsperioda er ferdig serialisert, kan antall feil hentast ut og leggast ved den serialiserte
 * representasjonen av rada for å markere kva rader det er dårlig datakvalitet på og ikkje.
 *
 * @author Tarjei Skorgenes
 */
class ErrorDetector {
    int antallFeil;

    ErrorDetector utfoer(final Consumer<Object> builder, final Underlagsperiode p,
                         final Function<Underlagsperiode, String> function) {
        try {
            builder.accept(function.apply(p));
        } catch (final Exception e) {
            antallFeil++;
            builder.accept("");
        }
        return this;
    }

    ErrorDetector multiple(final Consumer<Object> builder, final Underlagsperiode p,
                           final Stream<Function<Underlagsperiode, String>> function) {
        function.forEach(f -> utfoer(builder, p, f));
        return this;
    }
}
