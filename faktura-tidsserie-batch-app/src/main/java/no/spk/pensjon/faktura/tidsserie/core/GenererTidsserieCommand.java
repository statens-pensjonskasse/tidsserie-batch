package no.spk.pensjon.faktura.tidsserie.core;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Feilhandtering;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;

/**
 * {@link GenererTidsserieCommand} er eit backend-uavhengig kommandobjekt som genererer ein tidsserie
 * for eit enkelt medlem.
 * <br>
 * Kommandoen er primært ein koordinator mellom følgjande tenester/fasader i og utanfor domenemodellen:
 * <ul>
 * <li>{@link TidsserieFactory}</li>
 * <li>{@link Tidsseriemodus}</li>
 * <li>{@link StorageBackend}</li>
 * <li>{@link TidsserieFacade}</li>
 * </ul>
 *
 * @author Tarjei Skorgenes
 */
public interface GenererTidsserieCommand {
    /**
     * Genererer ein ny tidsserie basert på <code>medlemsdata</code> og avtale- og referansedata frå
     * {@link TidsserieFactory}.
     * <br>
     * Tidsserien blir avgrensa til å ikkje strekke seg lenger enn den angitte observasjonsperioda.
     * <br>
     * Alle feil som oppstår på stillingsforholdnivå, det vil seie som ein del av underlagsoppbygginga og -prosesseringa,
     * blir delegert til <code>feilhandtering</code> før prosesseringa går vidare til medlemmets neste stillingsforhold
     * eller neste medlemm viss det ikkje er fleire stillingar å behandle for medlemmet det feila på.
     * <br>
     * Feil som oppstår i forkant av underlagsoppbygginga, som endel av prosesseringa av medlemsdatane, fører til at heile
     * tidsserien for det aktuelle medlemmet feilar, slike feil blir ikkje delegert vidare til <code>feilhandtering</code>.
     *
     * @param medlemsdata serialiserte medlemsdata for eit enkelt medlem
     * @param periode observasjonsperioda som bestemmer yttergrensene for tidsserien sine underlagsperioder sine
     * frå og med- og til og med-datoar
     * @param feilhandtering feilhandteringsstrategien som vil bli bedt om å handtere alle feil på stillingsforholdnivå
     * @param serienummer serienummer som alle eventar som blir sendt vidare for persistering skal tilhøyre
     * @throws RuntimeException dersom deserialiseringa av <code>medlemsdata</code> eller prosessering på medlemsnivå feilar
     */
    void generer(List<List<String>> medlemsdata, Observasjonsperiode periode,
            Feilhandtering feilhandtering, long serienummer);
}
