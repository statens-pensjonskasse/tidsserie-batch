package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.loennsdata.Omregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

interface CsvOversetter<T extends Tidsperiode<?>> {
    boolean supports(List<String> rad);

    T oversett(List<String> rad);
}
