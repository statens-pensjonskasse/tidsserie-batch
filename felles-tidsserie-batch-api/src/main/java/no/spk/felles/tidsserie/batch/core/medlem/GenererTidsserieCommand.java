package no.spk.felles.tidsserie.batch.core.medlem;

import java.util.List;

import no.spk.felles.tidsserie.batch.core.StorageBackend;
import no.spk.felles.tidsserie.batch.core.TidsperiodeFactory;
import no.spk.felles.tidsserie.batch.core.Tidsseriemodus;

/**
 * {@link GenererTidsserieCommand} er eit backend-uavhengig kommandobjekt som genererer ein tidsserie
 * for eit enkelt medlem.
 * <br>
 * Kommandoen er primært ein koordinator mellom følgjande tenester/fasader i og utanfor domenemodellen:
 * <ul>
 * <li>{@link TidsperiodeFactory}</li>
 * <li>{@link Tidsseriemodus}</li>
 * <li>{@link StorageBackend}</li>
 * </ul>
 *
 * @author Tarjei Skorgenes
 */
public interface GenererTidsserieCommand {
    /**
     * Genererer ein ny tidsserie basert på <code>medlemsdata</code> og avtale- og referansedata frå
     * {@link TidsperiodeFactory}.
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
     * @param key Identifikator for dette medlemmet
     * @param medlemsdata serialiserte medlemsdata for eit enkelt medlem
     * frå og med- og til og med-datoar
     * @param tidsserieContext Agentvalues som inneholder serienummer og som kan sende feilmeldingen til Hazelcast contexten.
     * @throws RuntimeException dersom deserialiseringa av <code>medlemsdata</code> eller prosessering på medlemsnivå feilar
     */
   void generer(String key, List<List<String>> medlemsdata,
            TidsserieContext tidsserieContext);
}
