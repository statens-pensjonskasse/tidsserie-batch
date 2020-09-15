package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import no.spk.felles.tidsserie.batch.core.medlem.GenererTidsserieCommand;

/**
 * {@link ProsesserPartisjon} er ansvarlig for å prosessere alle medlemmar som inngår i ein bestemt partisjon.
 * <p>
 * Prosesseringa delegerer sjølve behandlinga av medlemmet til {@link GenererTidsserieCommand}, men tar seg sjølv
 * av å handtere eventuelle feil som den kastar.
 * <p>
 * I tillegg er klassa ansvarlig for å notifisere {@link CompositePartisjonListener lyttarar} om at behandling av
 * partisjonen blir {@link CompositePartisjonListener#partisjonInitialisert(Partisjonsnummer, Context) starta}.
 * <p>
 * Alle feil som blir kasta, enten frå kommandoen eller ein eller fleire av lyttarane, vil bli delegert til
 * {@link Context#emitError(Throwable)}. I tillegg vil feil frå kommandoen bli delegert til {@link MedlemFeilarListener}
 * for å informere klienten direkte, f.eks. for å logge eventuelle feilmeldingar for medlemmet.
 *
 * @see MedlemFeilarListener
 * @see CompositePartisjonListener
 */
class ProsesserPartisjon {
    private final Nodenummer node;
    private final Partisjon partisjon;
    private final Context meldingar;

    ProsesserPartisjon(final Nodenummer node, final Partisjon partisjon) {
        this.node = requireNonNull(node, "node er påkrevd, men var null");
        this.partisjon = requireNonNull(partisjon, "partisjon er påkrevd, men var null");
        this.meldingar = new Context(partisjon.nummer());
    }

    Context prosesser(
            final GenererTidsserieCommand kommando,
            final CompositePartisjonListener partisjonListener,
            final MedlemFeilarListener medlemFeilarListener
    ) {
        meldingar.inkluderFeilmeldingarFrå(
                () ->
                        partisjonListener.partisjonInitialisert(
                                partisjon.nummer(),
                                meldingar
                        )
        );
        partisjon
                .forEach(
                        (medlemsId, medlemsdata) ->
                                prosesserMedlem(
                                        kommando,
                                        medlemsId,
                                        medlemsdata,
                                        medlemFeilarListener
                                )
                );
        return meldingar;
    }

    private void prosesserMedlem(
            final GenererTidsserieCommand kommando,
            final String medlemsId,
            final List<List<String>> medlemsdata,
            final MedlemFeilarListener listener
    ) {
        meldingar.emit("medlem");
        try {
            kommando.generer(
                    medlemsId,
                    medlemsdata,
                    meldingar
            );
        } catch (final RuntimeException | Error e) {
            meldingar.emitError(e);
            meldingar.inkluderFeilmeldingarFrå(
                    () -> listener.medlemFeila(medlemsId, e)
            );
        }
    }

    interface CompositePartisjonListener {
        void partisjonInitialisert(Partisjonsnummer nummer, Context meldingar);
    }

    interface MedlemFeilarListener {
        void medlemFeila(String medlemsId, final Throwable t);
    }
}
