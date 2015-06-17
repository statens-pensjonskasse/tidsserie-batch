package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(parameterSummary).contains("-kjoeretid: " + programArguments.getKjoeretid());
        assertThat(parameterSummary).contains("-sluttid: " + programArguments.getSluttidspunkt());
        assertThat(parameterSummary).doesNotContain("-help");
    }
}