package no.spk.felles.tidsserie.batch.main;

import static java.time.format.DateTimeFormatter.ofPattern;
import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.availableProcessors;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import no.spk.felles.tidsserie.batch.main.input.ProgramArguments;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ConsoleViewIT {

    @RegisterExtension
    public final StandardOutputAndError console = new StandardOutputAndError();

    private final ConsoleView view = new ConsoleView().overstyr(klokke());

    private final ProgramArguments arguments = new ProgramArguments();

    private LocalDateTime tidspunkt = LocalDateTime.now();

    @BeforeEach
    void _before() {
        stillKlokka("1970-01-01 - 00:00:00");
    }

    @AfterEach
    void _after() {
        console.assertStandardError().isEmpty();
    }

    @Test
    void skal_inkludere_tidspunkt_ved_oppstart() {
        stillKlokka("2018-08-13 - 01:05:28");

        informerOmOppstart();

        assertStandardOutput().contains("Tidsserie-batch startet 2018-08-13 - 01:05:28");
    }

    @Test
    void skal_inkludere_oppsummering_av_kommandolinjeargument_ved_oppstart() {
        informerOmOppstart();

        assertStandardOutput().contains("Følgende programargumenter blir brukt:");
        assertStandardOutput()
                .contains("-fraAar: ")
                .contains("-tilAar: ")
                .contains("-n: ")
                .contains("-slettLog: ")
                .contains("-kjoeretid: ")
                .contains("-sluttid: ")
                .contains(arguments.toString())
        ;
    }

    @Test
    void skal_advare_ved_oppstart_dersom_antall_prosessorar_er_lik_antall_tilgjengelige_prosessorar() {
        arguments.antallProsessorar(availableProcessors());

        informerOmOppstart();

        assertStandardOutput().contains("ADVARSEL: Antall noder angitt er lik antall prosessorer på serveren.");
    }

    private AbstractCharSequenceAssert<?, String> assertStandardOutput() {
        return console.assertStandardOutput();
    }

    private void informerOmOppstart() {
        view.informerOmOppstart(arguments);
    }

    private Supplier<LocalDateTime> klokke() {
        return () -> tidspunkt;
    }

    private void stillKlokka(final String tidspunkt) {
        this.tidspunkt = LocalDateTime.parse(tidspunkt, ofPattern("yyyy-MM-dd - HH:mm:ss"));
    }
}