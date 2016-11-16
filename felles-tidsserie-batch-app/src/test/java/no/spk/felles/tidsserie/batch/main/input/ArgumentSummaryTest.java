package no.spk.felles.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThat;

import no.spk.faktura.input.ArgumentSummary;

import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class ArgumentSummaryTest {
    @Test
    public void testArgumentSummaryForDefaultArguments() throws Exception {
        ProgramArguments programArguments = new ProgramArguments();
        String parameterSummary = ArgumentSummary.createParameterSummary(programArguments);
        assertThat(parameterSummary).contains("-fraAar: " + programArguments.fraAar);
        assertThat(parameterSummary).contains("-tilAar: " + programArguments.tilAar);
        assertThat(parameterSummary).doesNotContain("-help");
    }
}