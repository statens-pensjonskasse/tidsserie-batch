package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.felles.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;

import org.junit.Rule;
import org.junit.Test;

public class ActivatorIT {
    @Rule
    public final ServiceRegistryRule registry = new ServiceRegistryRule();


    @Test
    public void skal_notifisere_om_manglande_kommando_uten_sjølv_å_feile() {
        medArgumenter(antallProsessorar(1));

        aktiver();

        final MedlemsdataBackend backend =
                new ServiceLocator(registry.registry())
                        .firstMandatory(MedlemsdataBackend.class);

        final Extensionpoint<TidsserieLivssyklus> livssyklus = new Extensionpoint<>(TidsserieLivssyklus.class, registry.registry());
        try {
            livssyklus
                    .invokeAll(l -> l.start(registry.registry()))
                    .orElseRethrowFirstFailure();

            final MedlemsdataUploader uploader = backend.uploader();
            uploader.append(
                    new Medlemslinje(
                            rad("Medlemmet", "Litt medlemsdata")
                    )
            );
            uploader.run();

            final Map<String, Integer> resultat = backend.lagTidsserie();
            assertThat(
                    resultat
            )
                    .hasSize(4)
                    .containsEntry("medlem", 1)
                    .containsEntry("errors", 1)
                    .containsEntry("errors_type_IngenGenererTidsserieKommandoRegistrertException", 1)
                    .containsEntry("errors_message_Det eksisterer ikkje noko teneste av type GenererTidsserieCommand i tenesteregisteret", 1);
        } finally {
            livssyklus
                    .invokeAll(l -> l.stop(registry.registry()))
                    .orElseRethrowFirstFailure();
        }
    }

    private void aktiver() {
        new Activator().aktiver(registry.registry());
    }

    private void medArgumenter(final AntallProsessorar antall) {
        registry.registrer(
                TidsserieBatchArgumenter.class,
                ArgumenterStub.medAntallProsessorar(antall)
        );
    }
}