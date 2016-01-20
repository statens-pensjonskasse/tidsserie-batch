package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.util.Optional.empty;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelperiode;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.BeregningsRegel;

/**
 * Aktuelle regler som skal benyttes av {@link Avtaleunderlagformat}
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagRegelsett implements Regelsett {
    @Override
    public Stream<Regelperiode<?>> reglar() {
        return Stream.of(
                prognoseperiode(new AarsfaktorRegel()),
                prognoseperiode(new AarsLengdeRegel()),
                prognoseperiode(new AntallDagarRegel())
        );
    }

    private Regelperiode<?> prognoseperiode(final BeregningsRegel<?> regel) {
        return new Regelperiode<>(dato("1917.01.01"), empty(), regel);
    }

}
