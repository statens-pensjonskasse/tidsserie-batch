package no.spk.tidsserie.batch.main.input;

import static no.spk.tidsserie.batch.core.UttrekksId.uttrekksId;

import no.spk.tidsserie.batch.core.UttrekksId;

import picocli.CommandLine.ITypeConverter;

public class UttrekksIdConverter implements ITypeConverter<UttrekksId> {
    @Override
    public UttrekksId convert(final String value) {
        return uttrekksId(value);
    }
}
