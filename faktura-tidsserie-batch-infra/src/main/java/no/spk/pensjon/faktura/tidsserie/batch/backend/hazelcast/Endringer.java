package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class Endringer extends ArrayList<List<String>> {
    private static final long serialVersionUID = -7074999943921522665L;

    Endringer(int rowCount) {
        super(rowCount);
    }

    Endringer(final Collection<List<String>> endringer) {
        super(endringer);
    }
}