package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.parallellisert;

import static java.util.Objects.requireNonNull;

import java.util.List;

import no.spk.premie.tidsserie.batch.core.medlem.GenererTidsserieCommand;

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
    private final Partisjon partisjon;
    private final Context context;

    ProsesserPartisjon(final Partisjon partisjon) {
        this.partisjon = requireNonNull(partisjon, "partisjon er påkrevd, men var null");
        this.context = new Context(partisjon.nummer());
    }

    Meldingar prosesser(
            final GenererTidsserieCommand kommando,
            final CompositePartisjonListener partisjonListener,
            final MedlemFeilarListener medlemFeilarListener
    ) {
        context.inkluderFeilmeldingarFrå(
                () ->
                        partisjonListener.partisjonInitialisert(
                                partisjon.nummer(),
                                context
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
        return context.meldingar();
    }

    private void prosesserMedlem(
            final GenererTidsserieCommand kommando,
            final String medlemsId,
            final List<List<String>> medlemsdata,
            final MedlemFeilarListener listener
    ) {
        context.emit("medlem");
        try {
            kommando.generer(
                    medlemsId,
                    medlemsdata,
                    context
            );
        } catch (final RuntimeException | Error e) {
            context.emitError(e);
            context.inkluderFeilmeldingarFrå(
                    () -> listener.medlemFeila(medlemsId, e)
            );
        }
    }
}
