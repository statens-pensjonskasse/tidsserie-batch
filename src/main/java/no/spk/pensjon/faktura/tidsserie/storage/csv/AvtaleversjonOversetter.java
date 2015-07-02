package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.Datoar;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;

public class AvtaleversjonOversetter extends ReflectiveCsvOversetter<AvtaleversjonCsv, Avtaleversjon> implements CsvOversetter<Avtaleversjon> {

    public AvtaleversjonOversetter() {
        super("AVTALEVERSJON", AvtaleversjonCsv.class);
    }

    @Override
    protected Avtaleversjon transformer(AvtaleversjonCsv csvRad) {
        return new Avtaleversjon(
                csvRad.fraOgMedDato.map(Datoar::dato).get(),
                csvRad.tilOgMedDato.map(Datoar::dato),
                csvRad.avtaleid.map(AvtaleId::valueOf).get(),
                csvRad.premiestatus.map(Premiestatus::valueOf).orElse(Premiestatus.UKJENT)
        );
    }
}
