package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.util.Optional;

import com.beust.jcommander.ParameterException;

/**
 * Validerer programargumenter som har avhengigheter til hverandre, f.eks. at et argument ikke kan v�re st�rre enn ett annet.
 *
 * @author Snorre E. Brekke - Computas
 * @see ProgramArgumentsFactory
 */
class PostParseValidation {
    private final ProgramArguments programArguments;

    PostParseValidation(ProgramArguments programArguments) {
        this.programArguments = programArguments;
    }

    void validate() throws ParameterException {
        if (programArguments.fraAar > programArguments.tilAar) {
            throw new ParameterException("'-fraAar' kan ikke v�re st�rre enn '-tilAar' (" +
                    programArguments.fraAar + " > " + programArguments.tilAar + ")");
        }

        if (programArguments.fraAvtale > programArguments.tilAvtale) {
            throw new ParameterException("'-fraAvtale' kan ikke v�re st�rre enn '-tilAvtale' (" +
                    programArguments.fraAvtale + " > " + programArguments.tilAvtale + ")");
        }

        if (!ingenDatabaseArgumenterAngitt() && !alleDatabaseArgumenterAngitt()) {
            throw new ParameterException(feilmelding());
        }
    }

    static String feilmelding() {
        return "B�de -jdbcUrl, -jdbcBrukernavn og -jdbcPassordfil m� ha en verdi dersom et eller flere av parameterne har f�tt angitt en verdi";
    }

    private boolean alleDatabaseArgumenterAngitt() {
        return programArguments.jdbcParameter().allMatch(Optional::isPresent);
    }

    private boolean ingenDatabaseArgumenterAngitt() {
        return programArguments.jdbcParameter().noneMatch(Optional::isPresent);
    }
}
