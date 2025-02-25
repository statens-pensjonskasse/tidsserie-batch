package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Function;

import no.spk.tidsserie.batch.core.Tidsseriemodus;
import no.spk.tidsserie.batch.core.UttrekksId;
import no.spk.tidsserie.batch.core.kommandolinje.AldersgrenseForSlettingAvLogKatalogar;
import no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;

class ArgumenterStub implements TidsserieBatchArgumenter {
    static ArgumenterStub medAntallProsessorar(final int antall) {
        return medAntallProsessorar(AntallProsessorar.antallProsessorar(antall));
    }

    static ArgumenterStub medAntallProsessorar(final AntallProsessorar antall) {
        return new ArgumenterStub() {
            @Override
            public AntallProsessorar antallProsessorar() {
                return antall;
            }
        };
    }

    @Override
    public AntallProsessorar antallProsessorar() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path uttrekkskatalog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path logkatalog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path utkatalog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AldersgrenseForSlettingAvLogKatalogar slettegrense() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Duration maksimalKjøretid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalTime avsluttFørTidspunkt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tidsseriemodus modus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void velgUttrekkVissIkkeAngitt(final Function<Path, UttrekksId> function) {
        throw new UnsupportedOperationException();
    }
}
