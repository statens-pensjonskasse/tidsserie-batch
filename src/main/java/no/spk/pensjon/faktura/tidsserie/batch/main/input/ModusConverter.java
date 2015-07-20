package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static no.spk.pensjon.faktura.tidsserie.batch.main.input.ModusValidator.feilmelding;

import com.beust.jcommander.IStringConverter;

/**
 * Konverterer modus-argument til {@link no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus}.
 *
 * @author Tarjei Skorgenes
 */
public class ModusConverter implements IStringConverter<Modus> {
    @Override
    public Modus convert(final String s) {
        return Modus
                .parse(s)
                .orElseThrow(() -> new IllegalArgumentException(feilmelding(s)));
    }
}
