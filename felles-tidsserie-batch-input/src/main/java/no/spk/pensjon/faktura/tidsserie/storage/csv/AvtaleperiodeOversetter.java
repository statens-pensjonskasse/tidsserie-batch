package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode.avtaleperiode;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Ordning;

public class AvtaleperiodeOversetter extends ReflectiveCsvOversetter<AvtaleCsv, Avtaleperiode> implements CsvOversetter<Avtaleperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public AvtaleperiodeOversetter() {
        super("AVTALE", AvtaleCsv.class);
    }

    @Override
    protected Avtaleperiode transformer(AvtaleCsv csvRad) {
        return avtaleperiode(csvRad.avtaleId.map(AvtaleId::valueOf).get())
                .fraOgMed(support.tilDato(csvRad.fraOgMed).get())
                .tilOgMed(support.tilDato(csvRad.tilOgMed))
                .arbeidsgiverId(csvRad.arbeidsgiverId.map(ArbeidsgiverId::valueOf).get())
                .ordning(csvRad.ordning.map(Ordning::valueOf))
                .bygg();
    }
}
