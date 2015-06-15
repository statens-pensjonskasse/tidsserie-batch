package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import com.beust.jcommander.ParameterException;

/**
 * Validerer programargumenter som har avhengigheter til hverandre, f.eks. at et argument ikke kan være større enn ett annet.
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
            throw new ParameterException("'-fraAar' kan ikke være større enn '-tilAar' (" +
                    programArguments.fraAar + " > " + programArguments.tilAar + ")");
        }

        new WritablePathValidator().validate("Batch utkatalog", programArguments.getGrunnlagsdataBatchKatalog());
    }
}
