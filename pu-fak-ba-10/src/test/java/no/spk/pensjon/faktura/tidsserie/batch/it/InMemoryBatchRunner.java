package no.spk.pensjon.faktura.tidsserie.batch.it;

import java.io.File;

import no.spk.pensjon.faktura.tidsserie.batch.main.ApplicationController;
import no.spk.pensjon.faktura.tidsserie.batch.main.TidsserieMain;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.Modus;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
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
    private final StandardOutputAndError outputAndError = new StandardOutputAndError();

    private final TidsserieMain batch;

    private int exitCode;

    InMemoryBatchRunner(final ServiceRegistry registry) {
        this.batch = new TidsserieMain(
                registry,
                exitCode -> this.exitCode = exitCode,
                new ApplicationController(registry)
        );
    }

    void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus) {

        try {
            outputAndError.before();
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
            outputAndError.after();
            outputAndError.assertBoolean("Har batchen avslutta OK?", exitCode == 0f).isTrue();
        }
    }
}
