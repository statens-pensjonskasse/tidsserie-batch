package no.spk.pensjon.faktura.tidsserie.core;

import static java.util.Objects.requireNonNull;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

public class BehandleMedlemCommand implements GenererTidsserieCommand {
    private final TidsserieFactory grunnlagsdata;
    private final StorageBackend lagring;
    private final Medlemsbehandler medlemsbehandler;

    /**
     * Konstruerer ein ny kommando som koordinerer mot dei angitte tenestene n�r tidsseriar pr medlem blir generert
     * av {@link #generer(List, Observasjonsperiode, Feilhandtering, long)}.
     *
     * @param grunnlagsdata tenesta som gir tilgang til grunnlagsdata som ikkje er medlemsspesifikke
     * @param lagring tenesta som lagrar observasjonane generert av
     * {@link Tidsseriemodus#createPublikator(TidsserieFacade, long, StorageBackend)}
     * @param medlemsbehandler delegat som regulerer kva {@link Regelsett} som skal benyttast, korleis
     * tidsserieobservasjonane skal byggast opp og hvilke medlem som skal behandlast
     * @throws NullPointerException viss nokon av argumenta er <code>null</code>
     * @since 2.0.0
     */
    public BehandleMedlemCommand(final TidsserieFactory grunnlagsdata,
            final StorageBackend lagring,
            final Medlemsbehandler medlemsbehandler) {
        this.grunnlagsdata = requireNonNull(grunnlagsdata, "grunnlagsdata er p�krevd, men manglar");
        this.lagring = requireNonNull(lagring, "publikator er p�krevd, men manglar");
        this.medlemsbehandler = requireNonNull(medlemsbehandler, "medlemsbehandler er p�krevd, men manglar");
    }

    @Override
    public void generer(final List<List<String>> medlemsdata, final Observasjonsperiode periode,
            final Feilhandtering feilhandtering, final long serienummer) {
        final TidsserieFacade tidsserie = grunnlagsdata.create(feilhandtering);
        final Medlemsdata medlem = grunnlagsdata.create(medlemsdata);
        if (medlemsbehandler.behandleMedlem(medlem)) {
            tidsserie.generer(
                    medlem,
                    periode,
                    medlemsbehandler.createPublikator(tidsserie, serienummer, lagring),
                    medlemsbehandler.referansedata(grunnlagsdata)
            );
        }
    }
}