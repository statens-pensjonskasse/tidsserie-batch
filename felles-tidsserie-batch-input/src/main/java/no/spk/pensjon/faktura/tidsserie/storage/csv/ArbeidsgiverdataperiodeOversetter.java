package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.time.LocalDate;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;

/**
 * Oversetter resultatfiler fra kundedata.sql til {@link Arbeidsgiverdataperiode}.
 * @author Snorre E. Breke - Computas
 */
public class ArbeidsgiverdataperiodeOversetter extends ReflectiveCsvOversetter<KundedataCsv, Arbeidsgiverdataperiode> implements CsvOversetter<Arbeidsgiverdataperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public ArbeidsgiverdataperiodeOversetter() {
        super("KUNDEDATA", KundedataCsv.class);
    }

    @Override
    protected Arbeidsgiverdataperiode transformer(KundedataCsv csvRad) {
        LocalDate fraOgMed = support.tilDato(csvRad.fraOgMedDato).get();
        return new Arbeidsgiverdataperiode(
                fraOgMed,
                support.tilDato(csvRad.tilOgMedDato),
                csvRad.orgnummer.map(Orgnummer::valueOf).get(),
                csvRad.arbeidsgiverId.map(ArbeidsgiverId::valueOf).get());
    }

}
