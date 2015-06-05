package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import java.util.Optional;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Util-klasse med metoden {@link #create} som produserer @{link ProgramArguments} fra en array med {@code String[]}
 *
 * @author Snorre E. Brekke - Computas
 * @see ProgramArguments
 */
public final class ProgramArgumentsFactory {
    private ProgramArgumentsFactory() {
    }

    /**
     * Tar en array med streng-argumenter og transformerer til en @{link ProgramArguments} representasjon av disse.
     * Metoden foretar validering og parsing av argumentene.
     * <p>
     * <b>NB!</b>
     * Printer ut bruk til konsoll dersom det er feil i argumentene,
     * eller argumentene inneholder flagg som indikerer at det ønskes hjelp.
     *
     * @param args typisk hentet fra main(String... args)
     * @return ProgramArguments generert fra args, dersom de kunne opprettes. {@link Optional#empty} dersom det er valideringsfeil.
     * @see JCommander
     */
    public static ProgramArguments create(final String... args) {
        return create(true, args);
    }

    /**
     * Transformerer argumentene til {@link ProgramArguments}, see {@link #create(String...)} for mer informasjon.
     * <p>
     * Denne metoden gjør det mulig å parse uten å utføre postvalidering av parameterne mot hverandre og er kun synlig
     * for intern bruk og tester.
     *
     * @param postValider <code>true</code> dersom postvalidering av parameterne skal uytføres, <code>false</code> ellers
     * @param args        typisk hentet fra main(String... args)
     * @return ProgramArguments generert fra args, dersom de kunne opprettes
     * @see #create(String...)
     */
    static ProgramArguments create(final boolean postValider, final String... args) {
        final ProgramArguments arguments = new ProgramArguments();
        final JCommander jCommander = new JCommander(arguments);
        try {
            jCommander.parse(args);
            if (postValider) {
            }
        } catch (final ParameterException exception) {
            throw new InvalidParameterException(jCommander, exception);
        }

        if (arguments.hjelp) {
            throw new UsageRequestedException(jCommander);
        }
        return arguments;
    }

    public static class UsageRequestedException extends RuntimeException {
        private final JCommander jCommander;

        UsageRequestedException(final JCommander jCommander) {
            this.jCommander = jCommander;
        }

        public String usage() {
            final StringBuilder usage = new StringBuilder();
            jCommander.usage(usage);
            return usage.toString();
        }
    }

    public static class InvalidParameterException extends RuntimeException {
        private final JCommander jCommander;

        InvalidParameterException(final JCommander jCommander, final ParameterException cause) {
            super(cause.getMessage());
            this.jCommander = jCommander;
        }

        public String usage() {
            final StringBuilder usage = new StringBuilder();
            jCommander.usage(usage);
            return usage.toString();
        }
    }
}
