package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataUploader;
import no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring.DatalagringStrategi;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

class PartisjonertMedlemsdataBackend implements MedlemsdataBackend, TidsserieLivssyklus {
    private final Partisjonstabell partisjonar = new Partisjonstabell();

    private final CompositePartisjonListener partisjonsListeners;
    private final GenererTidsserieCommand kommando;
    private final AntallProsessorar antallNoder;
    private final KommandoKjoerer<Meldingar> kommandoKjører;
    private final MedlemFeilarListener medlemFeilarListener;
    private final DatalagringStrategi datalagringStrategi;

    private final PartisjonertMedlemsdataOpplaster partisjonertOpplaster;

    public PartisjonertMedlemsdataBackend(
            final AntallProsessorar antallNoder,
            final KommandoKjoerer<Meldingar> kommandoKjører,
            final CompositePartisjonListener partisjonsListeners,
            final GenererTidsserieCommand kommando,
            final MedlemFeilarListener medlemFeilarListener,
            final DatalagringStrategi datalagringStrategi,
            final PartisjonertMedlemsdataOpplaster partisjonertOpplaster
    ) {
        this.kommandoKjører = requireNonNull(kommandoKjører, "kommandoKjører er påkrevd, men var null");
        this.antallNoder = requireNonNull(antallNoder, "antallNoder er påkrevd, men var null");
        this.partisjonsListeners = requireNonNull(partisjonsListeners, "partisjonsListeners er påkrevd, men var null");
        this.kommando = requireNonNull(kommando, "kommando er påkrevd, men var null");
        this.medlemFeilarListener = requireNonNull(medlemFeilarListener, "medlemFeilarListener er påkrevd, men var null");
        this.datalagringStrategi = requireNonNull(datalagringStrategi, "datalagringStrategi er påkrevd, men var null");
        this.partisjonertOpplaster = requireNonNull(partisjonertOpplaster, "partisjonertOpplaster er påkrevd, men var null");
    }

    @Override
    public void stop(final ServiceRegistry registry) {
        partisjonar.clear();
    }

    @Override
    public void start() {
    }

    @Override
    public MedlemsdataUploader uploader() {
        return new Uploader(partisjonar, datalagringStrategi);
    }

    @Override
    public Map<String, Integer> lagTidsserie() {
        try (final KommandoKjoerer<Meldingar> kjoerer = this.kommandoKjører) {
            return lagTidsserie(kjoerer);
        }
    }

    Map<String, Integer> lagTidsserie(final KommandoKjoerer<Meldingar> prosessering) {
        return
                fordelPartisjonarPåNoder(partisjonar)
                        .startParallellprosessering(
                                prosessering,
                                kommando,
                                partisjonsListeners,
                                medlemFeilarListener,
                                partisjonertOpplaster)
                        .map(ProsesserNode.AsyncResultat::ventPåResultat)
                        .reduce(
                                new Meldingar(),
                                Meldingar::merge
                        )
                        .toMap();
    }

    void put(final String key, final byte[] data, final DatalagringStrategi datalagringStrategi) {
        partisjonar.put(key, data, datalagringStrategi);
    }

    // For test-usage only
    AntallProsessorar antallProsessorar() {
        return antallNoder;
    }

    private LastbalansertePartisjonar fordelPartisjonarPåNoder(final Partisjonstabell partisjonstabell) {
        final int antallNoder = Math.toIntExact(
                antallProsessorar().stream().count()
        );
        return LastbalansertePartisjonar.lastbalanser(
                partisjonstabell,
                antallProsessorar()
                        .stream()
                        .mapToObj(
                                nummer -> new Nodenummer(
                                        nummer,
                                        antallNoder
                                )
                        )
        );
    }
}
