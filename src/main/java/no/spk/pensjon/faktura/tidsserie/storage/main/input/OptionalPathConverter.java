package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.beust.jcommander.IStringConverter;

public class OptionalPathConverter implements IStringConverter<Optional<Path>> {
    @Override
    public Optional<Path> convert(final String s) {
        return Optional.ofNullable(s).map(Paths::get);
    }
}
