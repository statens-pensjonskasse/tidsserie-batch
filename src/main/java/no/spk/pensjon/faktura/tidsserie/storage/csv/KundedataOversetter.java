package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.time.LocalDate;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Kundedataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Orgnummer;

/**
 * Oversetter resultatfiler fra kundedata.sql til {@link Kundedataperiode}.
 * @author Snorre E. Breke - Computas
 */
public class KundedataOversetter extends ReflectiveCsvOversetter<KundedataCsv, Kundedataperiode> implements CsvOversetter<Kundedataperiode> {

    private final OversetterSupport support = new OversetterSupport();

    public KundedataOversetter() {
        super("KUNDEDATA", KundedataCsv.class);
    }

    @Override
    protected Kundedataperiode transformer(KundedataCsv csvRad) {
        LocalDate fraOgMed = support.tilDato(csvRad.fraOgMedDato).get();
        return new Kundedataperiode(
                fraOgMed,
                support.tilDato(csvRad.tilOgMedDato),
                csvRad.orgnummer.map(Orgnummer::valueOf).get(),
                csvRad.arbeidsgiverId.map(ArbeidsgiverId::valueOf).get());
    }

}
