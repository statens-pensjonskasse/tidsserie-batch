package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;
import no.spk.felles.tidsserie.batch.core.medlem.TidsserieContext;

/**
 * {@link ProsesserNode} er ansvarlig for å prosessere alle partisjonar som {@link LastbalansertePartisjonar lastbalanseringa}
 * har tildelt ei bestemt node.
 * <p>
 * Prosesseringa av alle partisjonar tildelt noda vil bli gjort på ein og samme tråd for å eliminere thread-contention og eliminere
 * behovet for låsing eller krysskommunikasjon på tvers av trådar/CPUar.
 * <p>
 * Prosesseringa vil bli skedulert via {@link KommandoKjoerer#start(Callable)} og vil returnere eit (potensielt) {@link AsyncResultat asynkront resultat} som det
 * kan ta litt tid å få eit ferdig svar frå.
 * <p>
 * Når prosesseringa er fullført vil {@link AsyncResultat#ventPåResultat()} returnere alle meldingar som har blitt produsert av {@link GenererTidsserieCommand#generer(String, List, TidsserieContext)},
 * {@link MedlemFeilarListener} og {@link CompositePartisjonListener} for dei prosesserte partisjonane og medlemmane.
 * <p>
 * Det blir ikkje gitt nokon garantiar om i kva rekkefølge partisjonane blir behandla, det vil vere ikkje-deterministisk.
 *
 * @see KommandoKjoerer
 * @see AsyncResultat
 * @see GenererTidsserieCommand
 * @see CompositePartisjonListener
 * @see MedlemFeilarListener
 */
class ProsesserNode {
    private final Set<Partisjon> partisjonar;
    private final GenererTidsserieCommand kommando;
    private final CompositePartisjonListener partisjonsListeners;
    private final MedlemFeilarListener medlemFeilarListener;
    private final PartisjonertMedlemsdataOpplaster partisjonertOpplaster;

    ProsesserNode(
            final Set<Partisjon> partisjonar,
            final GenererTidsserieCommand kommando,
            final CompositePartisjonListener partisjonsListeners,
            final MedlemFeilarListener medlemFeilarListener,
            final PartisjonertMedlemsdataOpplaster partisjonertOpplaster) {
        this.partisjonar = requireNonNull(partisjonar, "partisjonar er påkrevd, men var null");
        this.kommando = requireNonNull(kommando, "kommando er påkrevd, men var null");
        this.partisjonsListeners = requireNonNull(partisjonsListeners, "partisjonsListeners er påkrevd, men var null");
        this.medlemFeilarListener = requireNonNull(medlemFeilarListener, "medlemFeilarListener er påkrevd, men var null");
        this.partisjonertOpplaster = requireNonNull(partisjonertOpplaster, "partisjonertOpplaster er påkrevd, men var null");
    }

    AsyncResultat start(final KommandoKjoerer<Meldingar> executor) {
        return new AsyncResultat(
                HarMeldingar.fra(
                        executor.start(
                                this::prosesserPartisjonar
                        )
                )
        );
    }

    private Meldingar prosesserPartisjonar() {
        return
                partisjonar
                        .stream()
                        .sorted(comparing(partisjon -> partisjon.nummer().index()))
                        .map(ProsesserPartisjon::new)
                        .map(
                                partisjon -> partisjon.prosesser(
                                        kommando,
                                        partisjonsListeners,
                                        medlemFeilarListener,
                                        partisjonertOpplaster)
                        )
                        .reduce(
                                new Meldingar(),
                                Meldingar::merge
                        );
    }


    static class AsyncResultat {
        private final HarMeldingar verdi;

        AsyncResultat(final HarMeldingar verdi) {
            this.verdi = requireNonNull(verdi, "verdi er påkrevd, men var null");
        }

        Meldingar ventPåResultat() {
            final Meldingar errors = new Meldingar();
            try {
                return verdi.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                errors.emitError(e);
                return errors;
            } catch (final ExecutionException e) {
                errors.emitError(e.getCause());
                return errors;
            }
        }


    }

    interface HarMeldingar {
        static HarMeldingar fra(final Future<Meldingar> future) {
            return future::get;
        }

        Meldingar get() throws ExecutionException, InterruptedException;
    }
}
