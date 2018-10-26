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

import com.beust.jcommander.ParameterException;

/**
 * Validerer programargumenter som har avhengigheter til hverandre, f.eks. at et argument ikke kan være større enn ett annet.
 * <p>
 * Før validering forsøker først {@link #validate(ProgramArguments)} å automatisk velge
 * det nyeste uttrekket fra underkatalogen(e) i innkatalogen til batchen.
 *
 * @author Snorre E. Brekke - Computas
 * @see TidsserieArgumentsFactory
 * @see ProgramArguments#velgUttrekkVissIkkeAngitt(Function)
 */
class PostParseValidation implements PostParseValidator<ProgramArguments> {
    public void validate(ProgramArguments programArguments) throws ParameterException {
        if (programArguments.fraAar > programArguments.tilAar) {
            throw new ParameterException("'-fraAar' kan ikke være større enn '-tilAar' (" +
                    programArguments.fraAar + " > " + programArguments.tilAar + ")");
        }

        programArguments.velgUttrekkVissIkkeAngitt(this::velgNyesteUttrekk);
        new ReadablePathValidator().validate("Utkatalog", programArguments.uttrekkskatalog());
    }

    private UttrekksId velgNyesteUttrekk(final Path innkatalog) {
        try (final Stream<Path> list = Files.list(innkatalog)) {
            return velgNyeste(list)
                    .orElseThrow(
                            () -> new ParameterException(
                                    format(
                                            "Det finnes ingen underkataloger med uttrekk av grunnlagsdata i %s.",
                                            innkatalog.toAbsolutePath()
                                    )
                            )
                    );
        } catch (final IOException e) {
            throw new ParameterException(e);
        }
    }
}
