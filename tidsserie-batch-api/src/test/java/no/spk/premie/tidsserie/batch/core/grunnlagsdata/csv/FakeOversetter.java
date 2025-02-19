package no.spk.premie.tidsserie.batch.core.grunnlagsdata.csv;

import static java.util.Optional.empty;

import java.time.LocalDate;
import java.util.List;

import no.spk.felles.tidsperiode.GenerellTidsperiode;
import no.spk.felles.tidsperiode.Tidsperiode;

class FakeOversetter implements CsvOversetter<Tidsperiode<?>> {
    @Override
    public boolean supports(final List<String> rad) {
        return true;
    }

    @Override
    public Tidsperiode<?> oversett(List<String> rad) {
        return new GenerellTidsperiode(LocalDate.now(), empty());
    }
}
