package no.spk.felles.tidsserie.batch.it;

import java.io.File;

import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.felles.tidsserie.batch.main.ApplicationController;
import no.spk.felles.tidsserie.batch.main.TidsserieMain;
import no.spk.felles.tidsserie.batch.main.input.Modus;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * {@link InMemoryBatchRunner} køyrer felles-tidsserie-batch direkte i samme prosess som testane køyrer via.
 * <br>
 * Intensjonen med dette er å dekke behovet for ei kodenær testsuite som smoke-testar at main-klassa og resten av
 * batchen, inkludert modusen den blir køyrt med, heng saman og produserer forventa format ut gitt statiske
 * grunnlagsdata inn.
 * <br>
 * Ulempa med å køyre batch og testar i samme prosess er at vi ikkje testar 100% realistisk i forhold til
 * classpathen som ein blir køyrt med. Testclasspathen kan påvirke batchen og skjule problem med hovedartifaktens
 * avhengigheitsgraf. For meir robust testing, sjå {@link OutOfProcessBatchRunner}.
 *
 * @author Tarjei Skorgenes
 */
class InMemoryBatchRunner implements FellesTidsserieBatch {
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

    @Override
    public void run(final File innKatalog, final File utKatalog, final Observasjonsperiode periode, final Modus modus) {
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
