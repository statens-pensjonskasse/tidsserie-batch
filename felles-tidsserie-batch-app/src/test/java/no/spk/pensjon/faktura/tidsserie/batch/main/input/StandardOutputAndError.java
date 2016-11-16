package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.rules.ExternalResource;

/**
 * Regel som fangar inn alt som blir printa på standard output og -error og gjer det tilgjengelig for testane uten å
 * støye til konsollen.
 *
 * @author Tarjei Skorgenes
 */
public class StandardOutputAndError extends ExternalResource {
    private ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private PrintStream oldStdout;
    private PrintStream oldStderror;

    @Override
    public void before() {
        oldStdout = System.out;
        oldStderror = System.err;

        clear();

        System.setOut(new PrintStream(stdout));

        System.setErr(new PrintStream(stderr));
    }

    @Override
    public void after() {
        System.setOut(oldStdout);
        System.setErr(oldStderror);
    }

    /**
     * Verifiserer alt innsamla innhold som testen har skreve til standard output.
     *
     * @return ein asserter med alt innhold skreve til standard output sidan testen starta
     * eller {@link #clear()} sist vart kalla
     */
    public AbstractCharSequenceAssert<?, String> assertStandardOutput() {
        return assertThat(stdout.toString()).as("standard output");
    }

    /**
     * Verifiserer alt innsamla innhold som testen har skreve til standard error.
     *
     * @return ein asserter med alt innhold skreve til standard error sidan testen starta
     * eller {@link #clear()} sist vart kalla
     */
    public AbstractCharSequenceAssert<?, String> assertStandardError() {
        return assertThat(stderr.toString()).as("standard error");
    }

    /**
     * Fjernar alle innsamla meldingar for standard output og -error.
     */
    public void clear() {
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
    }

    /**
     * Lagar ein assertion med angitt melding som beskrivelse og angitt verdi som den som blir asserta.
     * <br>
     * I tillegg til {@code message} blir meldinga utvida med informasjon om kva som er skreve til standard output
     * og standard error.
     *
     * @param message første linje i asserten sin beskrivelse
     * @param value   verdien som skal assertast
     * @return ein ny assert for verdien
     */
    public AbstractBooleanAssert<?> assertBoolean(final String message, final boolean value) {
        return assertThat(value)
                .as(
                        message + "\n"
                                + "\nStandard output:\n" + stdout
                                + "\nStandard error:\n" + stderr
                )
                .isTrue();
    }
}
