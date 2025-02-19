package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;

import no.spk.premie.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.premie.tidsserie.batch.core.medlem.Medlemslinje;

class Uploader implements MedlemsdataUploader {
    private final List<Medlemslinje> medlemsdata = new ArrayList<>();

    private final Partisjonstabell partisjonstabell;

    Uploader(final Partisjonstabell partisjonstabell) {
        this.partisjonstabell = requireNonNull(partisjonstabell, "partisjonstabell er påkrevd, men var null");
    }

    @Override
    public void append(final Medlemslinje linje) {
        medlemsdata.add(linje);
    }

    @Override
    public void run() {
        if (medlemsdata.isEmpty()) {
            throw new OpplastingAvMedlemsdataKreverMinst1RadException();
        }
        if (inneheldMeirEnn1Medlem()) {
            throw new ForskjelligeMedlemmarForsoektLastaOppSammenException();
        }
        partisjonstabell.put(
                medlemsdata
                        .get(0)
                        .medlem()
                        .toString(),
                this.medlemsdata
                        .stream()
                        .map(Medlemslinje::data)
                        .toList()
        );
        this.medlemsdata.clear();
    }

    private boolean inneheldMeirEnn1Medlem() {
        return medlemsdata
                .stream()
                .map(Medlemslinje::medlem)
                .collect(toSet())
                .size() > 1;
    }
}
