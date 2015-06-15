package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import no.spk.pensjon.faktura.tidsserie.batch.Foedselsnummer;
import no.spk.pensjon.faktura.tidsserie.batch.Medlemslinje;
import no.spk.pensjon.faktura.tidsserie.batch.MedlemsdataUploader;
import no.spk.pensjon.faktura.tidsserie.batch.ReferansedataService;

import com.hazelcast.core.IMap;

class UploadCommand implements MedlemsdataUploader {
    private final List<Medlemslinje> data = new ArrayList<>();

    private final Server server;
    private final IMap<String, List<List<String>>> medlemsdata;

    UploadCommand(final Server server, final IMap<String, List<List<String>>> medlemsdata) {
        this.server = server;
        this.medlemsdata = medlemsdata;
    }

    @Override
    public void append(final Medlemslinje linje) {
        data.add(linje);
    }

    @Override
    public void run() {
        final Foedselsnummer medlem = data.get(0).medlem();

        final Endringer e = new Endringer(data.stream().map(Medlemslinje::data).collect(toList()));
        data.clear();

        medlemsdata.putAsync(
                medlem.toString(),
                e,
                0,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void registrer(final ReferansedataService service) {
        server.registrer(service);
    }
}
