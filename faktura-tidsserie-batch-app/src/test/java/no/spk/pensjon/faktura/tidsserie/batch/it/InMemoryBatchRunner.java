package no.spk.pensjon.faktura.tidsserie.batch.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import no.spk.pensjon.faktura.tidsserie.batch.main.TidsserieMain;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link InMemoryBatchRunner} køyrer faktura-tidsserie-batch direkte utan å gå out of process slik som andre batchar blir testa i
 * faktura-integrasjonstester.
 * <br>
 * Intensjonen med dette er å dekke behovet for ei kodenær testsuite som smoke-testar at main-klassa og resten av batchen, inkludert modusen den blir køyrt
 * med, heng saman og produserer forventa format ut gitt statiske grunnlagsdata inn.
 *
 * @author Tarjei Skorgenes
 */
class InMemoryBatchRunner {
    private final TidsserieMain batch;

    private int exitCode;

    InMemoryBatchRunner(final ServiceRegistry registry) {
        this.batch = new TidsserieMain(
                registry,
                exitCode -> this.exitCode = exitCode
        );
    }

    void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus) {
        final PrintStream oldOut = System.out;
        final PrintStream oldError = System.err;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final ByteArrayOutputStream error = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(error));
            System.setOut(new PrintStream(output));
            batch.run(
                    "-i", innKatalog.toString(),
                    "-o", utKatalog.getPath(),
                    "-log", utKatalog.getPath(),
                    "-b", modus.toString(),
                    "-m", modus.kode(),
                    "-fraAar", "" + periode.fraOgMed().getYear(),
                    "-tilAar", "" + periode.tilOgMed().get().getYear(),
                    "-n", "1" // Speedar opp køyringa astronomisk mykje sidan vi ikkje ønskjer å vente på slave-shutdown med failover og partisjons-rebalansering
            );
        } finally {
            System.setOut(oldOut);
            System.setErr(oldError);
        }
        assertThat(
                exitCode == 0
        )
                .as("Har batchen avslutta OK?\n"
                        + "Exit code: " + exitCode
                        + "\nStandard output:\n" + output
                        + "\nStandard error:\n" + error
                )
                .isTrue();
    }
}
