package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import no.spk.felles.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.felles.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.felles.tidsserie.batch.core.medlem.PartisjonsListener;
import no.spk.felles.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.felles.tidsserie.batch.core.registry.Plugin;
import no.spk.felles.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements Plugin {
    @Override
    public void aktiver(final ServiceRegistry registry) {
        final ServiceLocator locator = new ServiceLocator(registry);

        final Extensionpoint<PartisjonsListener> partisjonsListeners = new Extensionpoint<>(
                PartisjonsListener.class,
                registry
        );

        final Extensionpoint<MedlemFeilarListener> medlemFeilarListeners = new Extensionpoint<>(
                MedlemFeilarListener.class,
                registry
        );

        final AntallProsessorar antallNoder = antallProsessorar(locator);
        final PartisjonertMedlemsdataBackend backend = new PartisjonertMedlemsdataBackend(
                antallNoder,
                KommandoKjoerer.velgFlertrådskjøring(
                        antallTrådar(antallNoder)
                ),
                (partisjonsnummer, context) -> context.inkluderFeilmeldingarFrå(
                        () ->
                                partisjonsListeners.invokeAll(
                                        listener ->
                                                listener.partitionInitialized(
                                                        context.getSerienummer()
                                                )
                                )
                ),
                wrapGenererTidsserieKommando(registry),
                (medlemsId, t) ->
                        medlemFeilarListeners
                                .invokeAll(listener -> listener.medlemFeila(medlemsId, t))
                                .orElseRethrowFirstFailure()
        );
        registry.registerService(
                MedlemsdataBackend.class,
                backend
        );
        registry.registerService(
                TidsserieLivssyklus.class,
                backend
        );
        registry.registerService(
                MedlemFeilarListener.class,
                new MedlemFeilarLogger()
        );
    }

    private int antallTrådar(final AntallProsessorar antallNoder) {
        return Math.toIntExact(
                antallNoder.stream().count()
        );
    }

    private AntallProsessorar antallProsessorar(final ServiceLocator locator) {
        return locator
                .firstMandatory(TidsserieBatchArgumenter.class)
                .antallProsessorar()
                ;
    }

    private GenererTidsserieCommand wrapGenererTidsserieKommando(final ServiceRegistry registry) {
        final Extensionpoint<GenererTidsserieCommand> kommandoar = new Extensionpoint<>(
                GenererTidsserieCommand.class,
                registry
        );
        return (medlemsId, medlemsdata, context) -> {
            if (!modusHarRegistrertEinKommando(registry)) {
                context.emitError(
                        new IngenGenererTidsserieKommandoRegistrertException()
                );
            }
            kommandoar.invokeFirst(
                    kommando ->
                            kommando.generer(
                                    medlemsId,
                                    medlemsdata,
                                    context
                            )
            )
                    .forEachFailure(context::emitError);
        };
    }

    private boolean modusHarRegistrertEinKommando(final ServiceRegistry registry) {
        return
                registry
                        .getServiceReference(GenererTidsserieCommand.class)
                        .isPresent();
    }

    static class MedlemFeilarLogger implements MedlemFeilarListener {
        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public void medlemFeila(final String medlemsId, final Throwable t) {
            log.warn("Periodisering av medlem {} feila: {}", medlemsId, t.getMessage());
            log.info("Feilkilde:", t);
        }
    }
}
