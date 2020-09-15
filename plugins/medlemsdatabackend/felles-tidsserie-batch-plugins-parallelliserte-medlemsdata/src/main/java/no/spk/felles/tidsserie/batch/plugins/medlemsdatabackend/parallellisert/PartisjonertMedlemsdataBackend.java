package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import java.util.HashMap;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.core.medlem.Medlemslinje;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

class PartisjonertMedlemsdataBackend implements MedlemsdataBackend, TidsserieLivssyklus {
    private final AntallProsessorar antallNoder;

    PartisjonertMedlemsdataBackend(final ServiceRegistry registry, final AntallProsessorar antallNoder) {
        this.antallNoder = antallNoder;
    }

    @Override
    public void start() {
    }

    @Override
    public MedlemsdataUploader uploader() {
        return new MedlemsdataUploader() {
            @Override
            public void append(final Medlemslinje linje) {
            }

            @Override
            public void run() {
            }
        };
    }

    @Override
    public Map<String, Integer> lagTidsserie() {
        return new HashMap<>();
    }


    // For test-usage only
    AntallProsessorar antallProsessorar() {
        return antallNoder;
    }
}
