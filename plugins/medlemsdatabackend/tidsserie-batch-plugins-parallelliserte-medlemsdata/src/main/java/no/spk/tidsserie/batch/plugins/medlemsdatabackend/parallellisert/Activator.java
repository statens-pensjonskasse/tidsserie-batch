package no.spk.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;

import java.util.List;

import no.spk.tidsserie.batch.core.TidsserieLivssyklus;
import no.spk.tidsserie.batch.core.kommandolinje.AntallProsessorar;
import no.spk.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.tidsserie.batch.core.medlem.MedlemsdataBackend;
import no.spk.tidsserie.batch.core.medlem.PartisjonsListener;
import no.spk.tidsserie.batch.core.medlem.TidsserieContext;
import no.spk.tidsserie.batch.core.registry.Extensionpoint;
import no.spk.tidsserie.batch.core.registry.Plugin;
import no.spk.tidsserie.batch.core.registry.ServiceLocator;
import no.spk.tidsserie.tjenesteregister.ServiceRegistry;

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
                nyWrapper(registry),
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

    GenererTidsserieCommand nyWrapper(final ServiceRegistry registry) {
        return new Wrapper(registry);
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

    static class MedlemFeilarLogger implements MedlemFeilarListener {
        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public void medlemFeila(final String medlemsId, final Throwable t) {
            log.warn("Periodisering av medlem {} feila: {}", medlemsId, t.getMessage());
            log.info("Feilkilde:", t);
        }
    }

    private static class Wrapper implements GenererTidsserieCommand {
        private final Extensionpoint<GenererTidsserieCommand> kommandoar;
        private final ServiceRegistry registry;

        public Wrapper(final ServiceRegistry registry) {
            this.registry = requireNonNull(
                    registry,
                    "registry er påkrevd, men var null"
            );
            this.kommandoar = new Extensionpoint<>(
                    GenererTidsserieCommand.class,
                    registry
            );
        }

        @Override
        public void generer(final String medlemsId, final List<List<String>> medlemsdata, final TidsserieContext context) {
            if (!modusHarRegistrertEinKommando(registry)) {
                context.emitError(
                        new IngenGenererTidsserieKommandoRegistrertException()
                );
            }
            kommandoar
                    .invokeFirst(
                            kommando ->
                                    kommando.generer(
                                            medlemsId,
                                            medlemsdata,
                                            context
                                    )
                    )
                    .orElseRethrowFirstFailure();
        }

        private boolean modusHarRegistrertEinKommando(final ServiceRegistry registry) {
            return
                    registry
                            .getServiceReference(GenererTidsserieCommand.class)
                            .isPresent();
        }
    }
}
