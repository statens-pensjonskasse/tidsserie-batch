package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.util.Optional;

import com.beust.jcommander.IStringConverter;

public class OptionalStringConverter implements IStringConverter<Optional<String>> {
    @Override
    public Optional<String> convert(final String s) {
        return Optional.ofNullable(s);
    }
}
