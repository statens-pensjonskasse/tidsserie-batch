package no.spk.tidsserie.batch.main.input;

import no.spk.tidsserie.input.InvalidParameterException;
import no.spk.tidsserie.input.ProgramArgumentsFactory;
import no.spk.tidsserie.input.UsageRequestedException;
import no.spk.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenterParser;
import no.spk.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

/**
 * Parser som støtter parsing av alle kommandolinjeargumenter som tidsserie-batch støtter direkte selv.
 * <p>
 * Applikasjoner som ønsker å tilby færre/flere/andre argumenter kan plugge inn sin egen håndtering i form av en separat
 * {@link TidsserieBatchArgumenter}.
 *
 * @see ProgramArguments
 * @see TidsserieBatchArgumenterParser
 */
public final class TidsserieArgumentsFactory implements TidsserieBatchArgumenterParser {
    private final ProgramArgumentsFactory<ProgramArguments> factory = new ProgramArgumentsFactory<>(
            ProgramArguments.class,
            new PostParseValidation()
    );

    public TidsserieArgumentsFactory() {
        Modus.autodetect();
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.1.0
     */
    @Override
    public TidsserieBatchArgumenter parse(final String... args) throws UgyldigKommandolinjeArgumentException, BruksveiledningSkalVisesException {
        try {
            return factory.create(args);
        } catch (final InvalidParameterException e) {
            throw new UgyldigKommandolinjeArgumentException(
                    e.getMessage(),
                    e::usage
            );
        } catch (final UsageRequestedException e) {
            throw new BruksveiledningSkalVisesException(e::usage);
        }
    }
}
