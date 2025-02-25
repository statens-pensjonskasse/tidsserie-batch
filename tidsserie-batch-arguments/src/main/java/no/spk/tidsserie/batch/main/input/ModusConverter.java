package no.spk.tidsserie.batch.main.input;

import static no.spk.tidsserie.batch.main.input.ModusValidator.feilmelding;

import picocli.CommandLine.ITypeConverter;

/**
 * Konverterer modus-argument til {@link Modus}.
 */
public class ModusConverter implements ITypeConverter<Modus> {
    @Override
    public Modus convert(final String s) {
        return Modus
                .parse(s)
                .orElseThrow(() -> new IllegalArgumentException(feilmelding(s)));
    }
}
