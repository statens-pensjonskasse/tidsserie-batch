package no.spk.felles.tidsserie.batch.main.input;

import static java.lang.String.format;
import static no.spk.felles.tidsserie.batch.core.UttrekksId.velgNyeste;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.faktura.input.PostParseValidator;
import no.spk.faktura.input.ReadablePathValidator;
import no.spk.felles.tidsserie.batch.core.UttrekksId;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/**
 * Validerer programargumenter som har avhengigheter til hverandre, f.eks. at et argument ikke kan være større enn ett annet.
 * <p>
 * Før validering forsøker først {@link #validate(ProgramArguments, CommandSpec)} å automatisk velge
 * det nyeste uttrekket fra underkatalogen(e) i innkatalogen til batchen.
 *
 * @see TidsserieArgumentsFactory
 * @see ProgramArguments#velgUttrekkVissIkkeAngitt(Function)
 * @see CommandSpec
 */
class PostParseValidation implements PostParseValidator<ProgramArguments> {

    public void validate(final ProgramArguments programArguments, final CommandSpec spec) throws ParameterException {
        if (programArguments.fraAar > programArguments.tilAar) {
            throw new ParameterException(
                    new CommandLine(spec),
                    "'-fraAar' kan ikke være større enn '-tilAar' (" +
                    programArguments.fraAar + " > " + programArguments.tilAar + ")"
            );
        }

        programArguments.velgUttrekkVissIkkeAngitt(u -> velgNyesteUttrekk(u, spec));
        new ReadablePathValidator().validate("Utkatalog", programArguments.uttrekkskatalog(), spec);
    }

    private UttrekksId velgNyesteUttrekk(final Path innkatalog, final CommandSpec spec) {
        try (final Stream<Path> list = Files.list(innkatalog)) {
            return velgNyeste(list)
                    .orElseThrow(
                            () -> new ParameterException(
                                    new CommandLine(spec),
                                    format(
                                            "Det finnes ingen underkataloger med uttrekk av grunnlagsdata i %s.",
                                            innkatalog.toAbsolutePath()
                                    )
                            )
                    );
        } catch (final IOException e) {
            throw new ParameterException(new CommandLine(spec), e.getMessage());
        }
    }
}
