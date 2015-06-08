package no.spk.pensjon.faktura.tidsserie.batch;

import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

/**
 * Hack for å unngå sirkulær avhengigheit mellom backend og frontend inntil vi finne på en god måte å propagere
 * avtale- og lønnsdata ut til alle hazelcast-nodene uten å bruke singletons eller liknande sjite.
 */
public interface ReferansedataService {
    Stream<Tidsperiode<?>> loennsdata();
    Stream<Tidsperiode<?>> finn(AvtaleId avtale);
}
