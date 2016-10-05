package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Optional.empty;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;

import java.time.LocalDate;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.felles.tidsperiode.underlag.reglar.Regelperiode;
import no.spk.felles.tidsperiode.underlag.reglar.Regelsett;
import no.spk.felles.tidsperiode.underlag.BeregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.TreigUUIDRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.UUIDRegel;

/**
 * Aktuelle regler som skal benyttes av {@link Avtaleunderlagformat}
 *
 * @author Snorre E. Brekke - Computas
 */
class AvtaleunderlagRegelsett implements Regelsett {
    @Override
    public Stream<Regelperiode<?>> reglar() {
        return Stream.of(
                periode(new AarsfaktorRegel()),
                periode(new AarsLengdeRegel()),
                periode(new AntallDagarRegel()),
                periode(UUIDRegel.class, new TreigUUIDRegel())
        );
    }

    private static <T> Regelperiode<?> periode(
            final Class<? extends BeregningsRegel<? extends T>> regelType, final BeregningsRegel<? extends T> regel) {
        return new Regelperiode<>(fraOgMed(), empty(), regelType, regel);
    }

    private static Regelperiode<?> periode(final BeregningsRegel<?> regel) {
        return new Regelperiode<>(fraOgMed(), empty(), regel);
    }

    private static LocalDate fraOgMed() {
        return dato("1917.01.01");
    }
}
