package no.spk.tidsserie.batch.main.input;

import static java.lang.Integer.parseInt;
import static no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;

import no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar;

import picocli.CommandLine.ITypeConverter;

public class AntallProsessorarConverter implements ITypeConverter<AntallProsessorar> {
    @Override
    public AntallProsessorar convert(final String value) {
        return antallProsessorar(parseInt(value));
    }
}
