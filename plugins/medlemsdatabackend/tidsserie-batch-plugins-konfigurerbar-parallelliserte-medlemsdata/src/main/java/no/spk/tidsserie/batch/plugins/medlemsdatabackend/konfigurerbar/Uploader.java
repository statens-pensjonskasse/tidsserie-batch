package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import no.spk.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DatalagringStrategi;

class Uploader implements MedlemsdataUploader {
    private static final String DELIMITER_COLUMN = ";";
    private static final String DELIMITER_ROW = "\n";

    private final List<Medlemslinje> medlemsdata = new ArrayList<>();

    private final Partisjonstabell partisjonstabell;
    private final DatalagringStrategi datalagringStrategi;

    Uploader(final Partisjonstabell partisjonstabell, DatalagringStrategi datalagringStrategi) {
        this.partisjonstabell = requireNonNull(partisjonstabell, "partisjonstabell er påkrevd, men var null");
        this.datalagringStrategi = requireNonNull(datalagringStrategi, "datalagringStrategi er påkrevd, men var null");
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
                medlemsdata
                        .stream()
                        .map(Medlemslinje::data)
                        .peek(this::valider)
                        .map(row -> join(DELIMITER_COLUMN, row))
                        .collect(joining(DELIMITER_ROW))
                        .getBytes(StandardCharsets.UTF_8),
                datalagringStrategi
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

    private void valider(final List<String> rad) {
        rad.forEach(this::valider);
    }

    private void valider(final String verdi) {
        if (verdi.contains(DELIMITER_COLUMN)) {
            throw new SemikolonSomDelAvVerdiIMedlemsdataStoettesIkkeException(verdi);
        }

        if (verdi.contains(DELIMITER_ROW)) {
            throw new LinjeskiftSomDelAvVerdiIMedlemsdataStoettesIkkeException(verdi);
        }
    }
}
