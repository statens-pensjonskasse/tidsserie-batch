package no.spk.felles.tidsserie.batch.backend.hazelcast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import no.spk.felles.tidsserie.batch.core.medlem.MedlemsId;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;

import com.hazelcast.core.IMap;

class UploadCommand implements MedlemsdataUploader {
    private final List<Medlemslinje> data = new ArrayList<>();

    private final IMap<String, List<List<String>>> medlemsdata;

    UploadCommand(final IMap<String, List<List<String>>> medlemsdata) {
        this.medlemsdata = medlemsdata;
    }

    @Override
    public void append(final Medlemslinje linje) {
        data.add(linje);
    }

    @Override
    public void run() {
        final MedlemsId medlem = data.get(0).medlem();

        final Endringer e = new Endringer(data.stream().map(Medlemslinje::data).toList());
        data.clear();

        medlemsdata.putAsync(
                medlem.toString(),
                e,
                0,
                TimeUnit.MILLISECONDS
        );
    }
}
