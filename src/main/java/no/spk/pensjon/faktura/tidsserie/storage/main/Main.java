package no.spk.pensjon.faktura.tidsserie.storage.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataService;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieBackendService;
import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend;
import no.spk.pensjon.faktura.tidsserie.batch.main.View;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;

/**
 * Batch som genererer tidsseriar for forenkla fakturering fastsats.
 * <br>
 * Batchen er avhengig av datasett generert av faktura-grunnlagsdata-batch. Datafilene den genererer blir lest inn av
 * faktura-tidsserie-batch, lasta opp til ein Hazelcast-basert in-memory backend og deretter brukt for å
 * generere tidsseriar på stillingsforholdnivå pr premieår pr observasjonsdato.
 * <br>
 * TODO: Fikse støtte for kommandolinjeargument for kontroll av batchens ytelses- og plasseringsmessige oppførsel,
 * feilhandtering, logging, brukarinfomering av køyrestatus og resultat osv.
 *
 * @author Snorre E. Brekke - Computas
 * @author Tarjei Skorgenes
 */
public class Main {
    private final TidsserieBackendService backend;

    private final GrunnlagsdataService overfoering;

    private int exitCode = 0;

    Main(final TidsserieBackendService backend, final GrunnlagsdataService overfoering) {
        this.backend = backend;
        this.overfoering = overfoering;
    }

    public static void main(String[] args) throws IOException {
        final View view = new View();
        final TidsserieBackendService backend = new HazelcastBackend();
        final GrunnlagsdataRepository input = new CSVInput(Paths.get("."));
        final GrunnlagsdataService overfoering = new GrunnlagsdataService(backend, input);

        final Main main = new Main(backend, overfoering);
        main.run(view, new File("output-XXX.csv"), new Aarstall(2005), new Aarstall(2015));

        System.exit(main.exitCode);
    }

    private void run(final View view, final File malFilnavn, final Aarstall fraOgMed, final Aarstall tilOgMed) {
        try {
            view.ryddarOppFilerFraaTidligereKjoeringer();
            // TODO: Fjern filer så vi ikkje endar opp med å bruke latterlig med plass :)

            view.verifisererInput();
            // TODO: Verifiser input-filene, er dei gyldige, sjekksummar korrekte osv

            view.startarBackend();
            backend.start();

            view.startarOpplasting();
            overfoering.lastOpp();
            view.opplastingFullfoert();

            view.startarTidsseriegenerering(malFilnavn, fraOgMed, tilOgMed);
            Map<String, Integer> meldingar = backend.lagTidsseriePaaStillingsforholdNivaa(
                    malFilnavn,
                    fraOgMed,
                    tilOgMed
            );
            // TODO: Lagre metadata, sjekksummar osv
            view.tidsseriegenereringFullfoert(meldingar);
        } catch (final IOException e) {
            view.fatalFeil(e);
            exitCode = 1;
        }
    }

}
