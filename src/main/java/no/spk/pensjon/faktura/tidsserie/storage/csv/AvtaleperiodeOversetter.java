package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;

public class AvtaleperiodeOversetter extends ReflectiveCsvOversetter<AvtaleCsv, Avtaleperiode> implements CsvOversetter<Avtaleperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public AvtaleperiodeOversetter() {
        super("AVTALE", AvtaleCsv.class);
    }

    @Override
    protected Avtaleperiode transformer(AvtaleCsv csvRad) {
        return new Avtaleperiode(
                support.tilDato(csvRad.fraOgMed).get(),
                support.tilDato(csvRad.tilOgMed),
                csvRad.avtaleId.map(AvtaleId::valueOf).get(),
                csvRad.arbeidsgiverId.map(ArbeidsgiverId::valueOf).get()
        );
    }
}
