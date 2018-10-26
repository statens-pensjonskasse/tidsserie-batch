package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.Integer.parseInt;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;

import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;

import com.beust.jcommander.IStringConverter;

public class AntallProsessorarConverter implements IStringConverter<AntallProsessorar> {
    @Override
    public AntallProsessorar convert(final String value) {
        return antallProsessorar(parseInt(value));
    }
}
