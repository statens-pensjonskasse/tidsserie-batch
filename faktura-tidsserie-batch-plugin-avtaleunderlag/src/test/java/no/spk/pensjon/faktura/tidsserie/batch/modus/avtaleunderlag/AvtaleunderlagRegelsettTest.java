package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static no.spk.felles.tidsperiode.AntallDagar.antallDagar;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.BeregningsRegel;
import no.spk.felles.tidsperiode.underlag.UnderlagsperiodeBuilder;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Aarsfaktor;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.UUIDRegel;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Test;

public class AvtaleunderlagRegelsettTest {
    private final AvtaleunderlagRegelsett reglar = new AvtaleunderlagRegelsett();

    private final UnderlagsperiodeBuilder builder = new UnderlagsperiodeBuilder()
            .fraOgMed(dato("1917.01.01"))
            .tilOgMed(dato("1917.01.31"));
    ;

    @Test
    public void skal_kunne_beregne_uuid() {
        assertBeregningsresultat(
                UUIDRegel.class,
                builder
        )
                .isInstanceOf(UUID.class);
    }

    @Test
    public void skal_kunne_beregne_antall_dagar() {
        assertBeregningsresultat(
                AntallDagarRegel.class,
                builder
        )
                .isEqualTo(antallDagar(31));
    }

    @Test
    public void skal_kunne_beregne_aarsfaktor() {
        assertBeregningsresultat(
                AarsfaktorRegel.class,
                builder.med(new Aarstall(1917))
        )
                .isInstanceOf(Aarsfaktor.class);
    }

    @Test
    public void skal_kunne_beregne_aarslengde() {
        assertBeregningsresultat(
                AarsLengdeRegel.class,
                builder.med(new Aarstall(1917))
        )
                .isEqualTo(antallDagar(365));
    }

    private <T> AbstractObjectAssert<?, T> assertBeregningsresultat(
            final Class<? extends BeregningsRegel<T>> regelType, final UnderlagsperiodeBuilder builder) {
        reglar.reglar().forEach(r -> r.annoter(builder));
        return assertThat(
                builder
                        .bygg()
                        .beregn(regelType)
        )
                .as("resultat fra " + regelType.getSimpleName());
    }
}