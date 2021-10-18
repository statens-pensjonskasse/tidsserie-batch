package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.medlemsdata;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.MedlemsdataBuilder.rad;
import static no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.Partisjonsnummer.partisjonsnummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.TidsserieContext;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DefaultDatalagringStrategi;

import org.assertj.core.api.MapAssert;
import org.junit.Test;

public class ProsesserPartisjonTest {
    private final Partisjon partisjon = new Partisjon(partisjonsnummer(1));

    private final ProsesserPartisjon prosessering = new ProsesserPartisjon(
            partisjon
    );

    @Test
    public void skal_behandle_medlemmar_i_deterministisk_rekkefølge_basert_på_rekkefølga_medlemmane_blir_lagt_til_i_partisjonen_første_gang() {
        partisjon.put("Ulrich Nielsen", medlemsdata(rad("Litt")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Katharina Nielsen", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Eva", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Hannah Kahnwald", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Adam", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Ulrich Nielsen", medlemsdata(rad("Enda meir")).medlemsdata(), new DefaultDatalagringStrategi());

        final List<String> behandlingsrekkefølge = new ArrayList<>();
        prosessering.prosesser(
                (key, medlemsdata, context) -> behandlingsrekkefølge.add(key),
                enPartisjonsLyttarSomAldriFeilar(),
                enMedlemFeilarLyttarSomAldriFeilar()
        );

        assertThat(behandlingsrekkefølge)
                .containsExactly(
                        "Ulrich Nielsen",
                        "Katharina Nielsen",
                        "Eva",
                        "Hannah Kahnwald",
                        "Adam"
                );
    }

    @Test
    public void skal_emitte_antall_medlemmar_behandla() {
        partisjon.put("Adam", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Eva", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Ulrich Nielsen", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Katharina Nielsen", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Hannah Kahnwald", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());

        assertMeldingar(
                enKommandoSomAldriFeilar(),
                enPartisjonsLyttarSomAldriFeilar(),
                enMedlemFeilarLyttarSomAldriFeilar()
        )
                .containsEntry("medlem", 5);
    }

    @Test
    public void skal_emitte_antall_feil() {
        partisjon.put("Adam", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Eva", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Ulrich Nielsen", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Katharina Nielsen", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Hannah Kahnwald", medlemsdata(rad()).medlemsdata(), new DefaultDatalagringStrategi());

        assertMeldingar(
                enKommandoSomAlltidFeilar(),
                enPartisjonsLyttarSomAldriFeilar(),
                enMedlemFeilarLyttarSomAldriFeilar()
        )
                .containsEntry("errors", 5);
    }

    @Test
    public void skal_prosessere_alle_medlemmar_sjølv_om_nokon_av_dei_feilar() {
        partisjon.put("Feilfritt medlem #1", medlemsdata(rad("Masse fine saker")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Inkonsistent medlem", medlemsdata(rad("Masse ræl og tøv")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Feilfritt medlem #2", medlemsdata(rad("Fleire fine saker")).medlemsdata(), new DefaultDatalagringStrategi());

        assertMeldingar(
                enKommandoSomFeilar("Inkonsistent medlem"::equals),
                enPartisjonsLyttarSomAldriFeilar(),
                enMedlemFeilarLyttarSomAldriFeilar()
        )
                .hasSize(4)
                .containsEntry("medlem", 3)
                .containsEntry("errors", 1)
                .containsEntry("errors_type_RuntimeException", 1)
                .containsEntry("errors_message_Whoopsie!", 1)
        ;
    }

    @Test
    public void skal_kun_notifisere_medlemslyttar_om_medlemmar_som_feilar() {
        partisjon.put("Feilfritt medlem #1", medlemsdata(rad("Masse fine saker")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Inkonsistent medlem", medlemsdata(rad("Masse ræl og tøv")).medlemsdata(), new DefaultDatalagringStrategi());
        partisjon.put("Feilfritt medlem #2", medlemsdata(rad("Fleire fine saker")).medlemsdata(), new DefaultDatalagringStrategi());

        final Map<String, Throwable> feilaMedlemmar = new HashMap<>();

        assertMeldingar(
                enKommandoSomFeilar("Inkonsistent medlem"::equals),
                enPartisjonsLyttarSomAldriFeilar(),
                feilaMedlemmar::put
        )
                .hasSize(4);

        assertThat(feilaMedlemmar)
                .hasSize(1)
                .hasEntrySatisfying(
                        "Inkonsistent medlem",
                        e -> assertThat(e).isInstanceOf(RuntimeException.class).hasMessage("Whoopsie!")
                );
    }

    @Test
    public void skal_aldri_avbryte_prosessering_dersom_medlemslyttar_feilar() {
        partisjon.put("Inkonsistent medlem", medlemsdata(rad("Masse ræl og tøv")).medlemsdata(), new DefaultDatalagringStrategi());

        assertMeldingar(
                enKommandoSomAlltidFeilar(),
                enPartisjonsLyttarSomAldriFeilar(),
                (medlemsId, feil) -> {
                    throw new UnsupportedOperationException("Oops: " + medlemsId + "!");
                }
        )
                .hasSize(6)
                .containsEntry("medlem", 1)
                .containsEntry("errors", 2)
                .containsEntry("errors_type_RuntimeException", 1)
                .containsEntry("errors_type_UnsupportedOperationException", 1)
                .containsEntry("errors_message_Whoopsie!", 1)
                .containsEntry("errors_message_Oops: Inkonsistent medlem!", 1)
        ;
    }

    @Test
    public void skal_fange_og_rapportere_feil_frå_partisjonslyttaren_utan_sjølv_å_feile() {
        partisjon.put("Agnes Nielsen", medlemsdata(rad("Født", "1910")).medlemsdata(), new DefaultDatalagringStrategi());

        assertMeldingar(
                enKommandoSomAldriFeilar(),
                (partisjonsnummer, meldingar) -> {
                    throw new IllegalStateException(
                            "Noko gjekk horribelt feil med " + partisjonsnummer
                    );
                },
                enMedlemFeilarLyttarSomAldriFeilar()
        )
                .hasSize(4)
                .containsEntry("medlem", 1)
                .containsEntry("errors", 1)
                .containsEntry("errors_type_IllegalStateException", 1)
                .containsEntry("errors_message_Noko gjekk horribelt feil med partisjon 1 av 271", 1)
        ;
    }

    private MapAssert<String, Integer> assertMeldingar(
            final GenererTidsserieCommand kommando,
            final CompositePartisjonListener partisjonListener,
            final MedlemFeilarListener medlemFeilarListener
    ) {
        return assertThat(
                prosessering.prosesser(
                        kommando,
                        partisjonListener,
                        medlemFeilarListener
                )
                        .toMap()
        );
    }

    private MedlemFeilarListener enMedlemFeilarLyttarSomAldriFeilar() {
        return new DontCare();
    }

    private CompositePartisjonListener enPartisjonsLyttarSomAldriFeilar() {
        return new DontCare();
    }

    private GenererTidsserieCommand enKommandoSomAldriFeilar() {
        return new DontCare();
    }

    private GenererTidsserieCommand enKommandoSomFeilar(final Predicate<String> skalFeile) {
        return new Whoopsie(skalFeile);
    }

    private GenererTidsserieCommand enKommandoSomAlltidFeilar() {
        return new Whoopsie(medlemsId -> true);
    }

    private static class Whoopsie implements GenererTidsserieCommand {
        private final Predicate<String> skalFeile;

        private Whoopsie(final Predicate<String> skalFeile) {
            this.skalFeile = requireNonNull(skalFeile, "skalFeile er påkrevd, men var null");
        }

        @Override
        public void generer(final String medlemsId, final List<List<String>> medlemsdata, final TidsserieContext context) {
            if (skalFeile.test(medlemsId))
                throw new RuntimeException("Whoopsie!");
        }
    }

    private static class DontCare implements CompositePartisjonListener, MedlemFeilarListener, GenererTidsserieCommand {
        @Override
        public void partisjonInitialisert(final Partisjonsnummer nummer, final Context meldingar) {
        }

        @Override
        public void medlemFeila(final String medlemsId, final Throwable feil) {
        }

        @Override
        public void generer(final String key, final List<List<String>> medlemsdata, final TidsserieContext context) {
        }
    }
}