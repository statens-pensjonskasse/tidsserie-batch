package no.spk.pensjon.faktura.tidsserie.batch;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.concat;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link GenererTidsserieCommand} er eit backend-uavhengig kommandobjekt som genererer ein tidsserie
 * for eit enkelt medlem.
 * <br>
 * Kommandoen er prim�rt ein koordinator mellom f�lgjande tenester/fasader i og utanfor domenemodellen:
 * <ul>
 * <li>{@link TidsserieFactory}</li>
 * <li>{@link Tidsseriemodus}</li>
 * <li>{@link StorageBackend}</li>
 * <li>{@link TidsserieFacade}</li>
 * </ul>
 *
 * @author Tarjei Skorgenes
 */
public class GenererTidsserieCommand {
    private final TidsserieFactory grunnlagsdata;
    private final StorageBackend lagring;
    private final Tidsseriemodus parameter;

    /**
     * Konstruerer ein ny kommando som koordinerer mot dei angitte tenestene n�r tidsseriar pr medlem blir generert
     * av {@link #generer(List, Observasjonsperiode, Feilhandtering)}.
     *
     * @param grunnlagsdata tenesta som gir tilgang til grunnlagsdata som ikkje er medlemsspesifikke
     * @param lagring       tenesta som lagrar observasjonane generert av
     *                      {@link Tidsseriemodus#create(TidsserieFacade, StorageBackend)}
     * @param modus         modusen som regulerer kva {@link Regelsett} som skal benyttast og korleis
     *                      tidsserieobservasjonane skal byggast opp
     * @throws NullPointerException viss nokon av argumenta er <code>null</code>
     */
    public GenererTidsserieCommand(final TidsserieFactory grunnlagsdata,
                                   final StorageBackend lagring,
                                   final Tidsseriemodus modus) {
        this.grunnlagsdata = requireNonNull(grunnlagsdata, "grunnlagsdata er p�krevd, men manglar");
        this.lagring = requireNonNull(lagring, "publikator er p�krevd, men manglar");
        this.parameter = requireNonNull(modus, "modus er p�krevd, men manglar");
    }

    /**
     * Genererer ein ny tidsserie basert p� <code>medlemsdata</code> og avtale- og referansedata fr�
     * {@link TidsserieFactory}.
     * <br>
     * Tidsserien blir avgrensa til � ikkje strekke seg lenger enn den angitte observasjonsperioda.
     * <br>
     * Alle feil som oppst�r p� stillingsforholdniv�, det vil seie som ein del av underlagsoppbygginga og -prosesseringa,
     * blir delegert til <code>feilhandtering</code> f�r prosesseringa g�r vidare til medlemmets neste stillingsforhold
     * eller neste medlemm viss det ikkje er fleire stillingar � behandle for medlemmet det feila p�.
     * <br>
     * Feil som oppst�r i forkant av underlagsoppbygginga, som endel av prosesseringa av medlemsdatane, f�rer til at heile
     * tidsserien for det aktuelle medlemmet feilar, slike feil blir ikkje delegert vidare til <code>feilhandtering</code>.
     *
     * @param medlemsdata    serialiserte medlemsdata for eit enkelt medlem
     * @param periode        observasjonsperioda som bestemmer yttergrensene for tidsserien sine underlagsperioder sine
     *                       fr� og med- og til og med-datoar
     * @param feilhandtering feilhandteringsstrategien som vil bli bedt om � handtere alle feil p� stillingsforholdniv�
     * @throws RuntimeException dersom deserialiseringa av <code>medlemsdata</code> eller prosessering p� medlemsniv� feilar
     */
    public void generer(final List<List<String>> medlemsdata, final Observasjonsperiode periode, final Feilhandtering feilhandtering) {
        final TidsserieFacade tidsserie = grunnlagsdata.create(feilhandtering);
        tidsserie.generer(
                grunnlagsdata.create(medlemsdata),
                periode,
                parameter.create(tidsserie, lagring),
                concat(
                        parameter.regelsett().reglar(),
                        grunnlagsdata.loennsdata()
                )
        );
    }
}
