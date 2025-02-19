package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer.partisjonsnummer;
import static no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.medlemsdata;
import static no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert.MedlemsdataBuilder.rad;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;

import org.junit.jupiter.api.Test;

class ProsesserNodeTest {
    @Test
    void skal_starte_ei_køyring_pr_kall() {
        final ProsesserNode prosessering = new ProsesserNode(
                Collections.singleton(new Partisjon(partisjonsnummer(1))),
                (key, medlemsdata, tidsserieContext) -> {
                },
                (nummer, meldingar) -> {
                },
                (medlemsId, t) -> {
                }
        );

        final KommandoKjoerer.Spion<Meldingar> spion = new KommandoKjoerer.Spion<>();

        prosessering.start(spion);
        assertThat(spion.tasks()).hasSize(1);

        prosessering.start(spion);
        assertThat(spion.tasks()).hasSize(2);

        prosessering.start(spion);
        assertThat(spion.tasks()).hasSize(3);
    }

    @Test
    void skal_aggregere_meldingar_frå_alle_partisjonar() {
        final ProsesserNode prosessering = new ProsesserNode(
                Stream.of(
                                partisjonsnummer(1),
                                partisjonsnummer(2)
                        )
                        .map(
                                partisjonsnummer -> {
                                    final Partisjon partisjon = new Partisjon(partisjonsnummer);
                                    partisjon.put(partisjon.toString(), medlemsdata(rad("ABCD")));
                                    return partisjon;
                                }
                        )
                        .collect(toSet()),
                (key, medlemsdata, context) -> {
                    switch ((int) context.getSerienummer()) {
                        case 1:
                            context.emitError(new NullPointerException("BCDE"));
                            break;
                        case 2:
                            context.emitError(new IndexOutOfBoundsException("ABCD"));
                            break;
                        default:
                            context.emitError(new Error());
                    }
                },
                (nummer, meldingar) -> {
                },
                (medlemsId, t) -> {
                }
        );
        assertThat(
                prosessering
                        .start(new KommandoKjoerer.SynkronKjoerer<>()).ventPåResultat()
                        .toMap()
        )
                .hasSize(6)
                .containsEntry("medlem", 2)
                .containsEntry("errors", 2)
                .containsEntry("errors_type_NullPointerException", 1)
                .containsEntry("errors_type_IndexOutOfBoundsException", 1)
                .containsEntry("errors_message_ABCD", 1)
                .containsEntry("errors_message_BCDE", 1)
        ;
    }

    @Test
    void skal_ikkje_feile_om_bakgrunnskøyringa_blir_avbrutt() {
        final ProsesserNode.AsyncResultat avbrutt = new ProsesserNode.AsyncResultat(
                () -> {
                    throw new InterruptedException();
                }
        );

        assertThat(
                avbrutt.ventPåResultat()
        )
                .satisfies(
                        meldingar -> assertThat(meldingar.toMap())
                                .hasSize(3)
                                .containsEntry("errors", 1)
                                .containsEntry("errors_type_InterruptedException", 1)
                                .containsEntry("errors_message_null", 1)
                );
    }

    @Test
    void skal_ikkje_feile_om_bakgrunnskøyringa_kræsjar() {
        final ProsesserNode.AsyncResultat kræsja = new ProsesserNode.AsyncResultat(
                () -> {
                    throw new ExecutionException(
                            new IllegalStateException("NB: Viktig feilmelding her\nOg noko meir informasjon...")
                    );
                }
        );

        assertThat(
                kræsja.ventPåResultat()
        )
                .satisfies(
                        meldingar -> assertThat(meldingar.toMap())
                                .hasSize(3)
                                .containsEntry("errors", 1)
                                .containsEntry("errors_type_IllegalStateException", 1)
                                .containsEntry("errors_message_NB: Viktig feilmelding her\nOg noko meir informasjon...", 1)
                );
    }

    @Test
    void skal_prosessere_partisjonane_i_deterministisk_rekkefølge_frå_lavaste_til_høgaste_partisjonsnummer() {
        final List<Partisjonsnummer> expected = Partisjonsnummer.stream().toList();
        final List<Partisjonsnummer> behandla = new ArrayList<>();

        final ProsesserNode prosessering = new ProsesserNode(
                expected
                        .stream()
                        .sorted(comparing(Partisjonsnummer::index).reversed())
                        .map(Partisjon::new)
                        .collect(toCollection(HashSet::new)),
                (key, medlemsdata, context) -> {
                },
                (partisjon, meldingar) -> behandla.add(partisjon),
                (medlemsId, t) -> {
                }
        );
        prosessering
                .start(new KommandoKjoerer.SynkronKjoerer<>())
                .ventPåResultat();

        assertThat(behandla).containsExactlyElementsOf(expected);
    }
}
