package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static java.util.Collections.reverseOrder;
import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdConstants.GRUNNLAGSDATA_PATTERN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import no.spk.faktura.input.PostParseValidator;
import no.spk.faktura.input.WritableDirectoryValidator;

import com.beust.jcommander.ParameterException;

/**
 * Validerer programargumenter som har avhengigheter til hverandre, f.eks. at et argument ikke kan være større enn ett annet.
 * Før validering forsøker først {@link #validate(ProgramArguments)} å finne grunnlagsdata-katalog basert på innkatalog til innkatalogen.
 * @author Snorre E. Brekke - Computas
 * @see TidsserieArgumentsFactory
 */
class PostParseValidation implements PostParseValidator<ProgramArguments>{
    public void validate(ProgramArguments programArguments) throws ParameterException {
        if (programArguments.getGrunnlagsdataBatchId() == null) {
            setDefaultBatchId(programArguments);
        }

        if (programArguments.fraAar > programArguments.tilAar) {
            throw new ParameterException("'-fraAar' kan ikke være større enn '-tilAar' (" +
                    programArguments.fraAar + " > " + programArguments.tilAar + ")");
        }

        new WritableDirectoryValidator().validate("Batch utkatalog", programArguments.getGrunnlagsdataBatchKatalog());
    }

    private void setDefaultBatchId(ProgramArguments arguments) {
        final Pattern pattern = GRUNNLAGSDATA_PATTERN;
        final Path innkatalog = arguments.getInnkatalog();
        try (final Stream<Path> list = Files.list(innkatalog)) {
            String grunnlagsdataBatchId = list
                    .map(f -> f.toFile().getName())
                    .filter(n -> pattern.matcher(n).matches())
                    .sorted(reverseOrder())
                    .findFirst()
                    .orElseThrow(() -> new ParameterException("Det finnes ingen batch-kataloger i " + innkatalog.toAbsolutePath() + "."));
            arguments.setGrunnlagsdataBatchId(grunnlagsdataBatchId);
        } catch (IOException e) {
            throw new ParameterException(e);
        }
    }
}
