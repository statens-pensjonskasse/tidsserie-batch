package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.Map;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.MedlemsdataOversetter;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

/**
 * Hack for � unng� sirkul�r avhengigheit mellom backend og frontend inntil vi finne p� en god m�te � propagere
 * avtale- og l�nnsdata ut til alle hazelcast-nodene uten � bruke singletons eller liknande sjite.
 */
public interface ReferansedataService {
    Stream<Tidsperiode<?>> loennsdata();

    Stream<Tidsperiode<?>> finn(AvtaleId avtale);

    /**
     * Returnerer oversettere for konvertering av medlemsdata fra CSV-format til
     * {@link no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata}.
     *
     * @return et nytt sett med oversettere for konvertering av medlemsdata fra CSV-format
     * @see no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata#Medlemsdata(java.util.List, java.util.Map)
     */
    Map<Class<?>, MedlemsdataOversetter<?>> medlemsdataOversettere();
}
