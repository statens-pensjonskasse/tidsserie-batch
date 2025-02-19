package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import java.util.Objects;


import no.spk.premie.tidsserie.batch.core.grunnlagsdata.LastOppGrunnlagsdataPartisjonertKommando;
import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.premie.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

class PartisjonertMedlemsdataOpplaster {

    private final ServiceRegistry serviceRegistry;

    PartisjonertMedlemsdataOpplaster(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = Objects.requireNonNull(serviceRegistry, "serviceRegistry er påkrevd, men var null");
    }

    void lastOppPartisjonertMedlemsdata(Partisjonsnummer partisjonsnummer) {
        final ServiceLocator locator = new ServiceLocator(serviceRegistry);
        locator.firstService(LastOppGrunnlagsdataPartisjonertKommando.class)
                .ifPresent(kommando -> kommando.lastOpp(serviceRegistry, partisjonsnummer.partisjonsnummer()));
    }
}
