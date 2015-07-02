package no.spk.pensjon.faktura.tidsserie.storage.csv;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;

/**
 * @author Snorre E. Breke - Computas
 */
public class ArbeidsgiverOversetter extends ReflectiveCsvOversetter<ArbeidsgiverCsv, Arbeidsgiverperiode> implements CsvOversetter<Arbeidsgiverperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public ArbeidsgiverOversetter() {
        super("ARBEIDSGIVER", ArbeidsgiverCsv.class);
    }

    @Override
    protected Arbeidsgiverperiode transformer(ArbeidsgiverCsv csvRad) {
        return new Arbeidsgiverperiode(
                support.tilDato(csvRad.innmeldtDato).get(),
                support.tilDato(csvRad.utmeldtDato),
                csvRad.arbeidsgiverId.map(ArbeidsgiverId::valueOf).get()
        );
    }

}
