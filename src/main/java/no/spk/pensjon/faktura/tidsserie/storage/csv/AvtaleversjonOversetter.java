package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CsvOversetter;

public class AvtaleversjonOversetter implements CsvOversetter<Avtaleversjon> {
    @Override
    public boolean supports(List<String> rad) {
        return rad.size() > 1 && "AVTALEVERSJON".equals(rad.get(0));
    }

    @Override
    public Avtaleversjon oversett(List<String> rad) {
        return new Avtaleversjon(
                read(rad, 2).map(Datoar::dato).get(),
                read(rad, 3).map(Datoar::dato),
                read(rad, 1).map(AvtaleId::valueOf).get(),
                read(rad, 5).map(Premiestatus::valueOf).orElse(Premiestatus.UKJENT)
        );
    }

    private Optional<String> read(final List<String> rad, final int index) {
        return ofNullable(rad.get(index)).map(String::trim).filter(t -> !t.isEmpty());
    }
}
