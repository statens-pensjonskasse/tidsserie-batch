package no.spk.pensjon.faktura.tidsserie.batch.core;

import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

/**
 * Extension-interface for tenestetilbydarar som ønskjer å agere før generering av tidsserie startar
 * og/eller rett etter den avsluttar.
 * <br>
 * Merk at registrering av nye livssyklustenester ikkje bør skje som en del av start elle stopp ettersom det vil kunne
 * gi udeterministisk oppførsel med tanke på om dei nye livssyklusane blir kalla eller ikkje.
 * <br>
 * Extensionpointet for livssyklusane gir følgjande garantiar til tjenester som implementerer det når det gjeld handtering
 * av feil som oppstår i tjenestene eller mellom kall til {@link #start(ServiceRegistry)} og {@link #stop(ServiceRegistry)}:
 * <ol>
 * <li>Alle tjenester kan anta at {@link #start(ServiceRegistry)} vil bli kalla sjølv om andre livssyklusar feilar på
 * kall til start</li>
 * <li>Alle tjenester kan anta at {@link #stop(ServiceRegistry)} vil bli kalla dersom  {@link #start(ServiceRegistry)} har blitt
 * kalla</li>
 * <li>Uforventa feil som oppstår i {@link #start(ServiceRegistry)} vil avbryte tidsseriegenereringa etter at dei to
 * første garantiane er oppfyllt</li>
 * <li>Uforventa feil som oppstår i {@link #stop(ServiceRegistry)} vil medføre at batchen blir markert som feila,
 * sjølv om tidsserien har blitt generert uten sjølv å feile</li>
 * </ol>
 *
 * @author Tarjei Skorgenes
 */
public interface TidsserieLivssyklus {
    /**
     * Generering av tidsserie skal til å starte opp.
     * <br>
     * På dette tidspunktet kan tenesta gå ut i frå at alle standardtenester er registrert i tenesteregisteret,
     * alt av grunnlagsdata er validert og gjort tilgjengelig in-memory, i tillegg er gamle CSV-filer og utdaterte
     * loggkatalogar sletta.
     * <br>
     * Tenestetilbydaren kan i denne callbacken kommunisere med andre tenester, registrere nye tenester eller overstyre
     * eksisterande tenester via tenesteregisteret.
     *
     * @param registry tenesteregisteret andre tenester som treng kan hentast via eller nye tenester kan registrerast
     */
    default void start(final ServiceRegistry registry) {
    }


    /**
     * Generering av tidsserie har nettopp blitt avslutta.
     * <br>
     * På dette tidspunktet kan tenesta gå ut i frå at tidsserien har blitt generert uten nokon fatale fail undervegs.
     * Innholdet i tidsserien har blitt persistert til lokal disk i batchens ut-katalog.
     * <br>
     * Tenestetilbydaren kan i denne callbacken kommunisere med andre tenester, registrere nye tenester eller overstyre
     * eksisterande tenester via tenesteregisteret.
     *
     * @param registry tenesteregisteret andre tenester som treng kan hentast via eller nye tenester kan registrerast
     */
    default void stop(final ServiceRegistry registry) {
    }

    /**
     * Genererer ein ny livssyklus som kun blir kalla før start av tidsseriegenereringa og som delegerer kallet til ein
     * annan funksjon.
     *
     * @param other den andre funksjonen som skal notifiserast om at tidsseriegenereringa skal til å starte
     * @return ein ny livssyklus
     */
    static TidsserieLivssyklus onStart(final Runnable other) {
        return new TidsserieLivssyklus() {
            @Override
            public void start(final ServiceRegistry registry) {
                other.run();
            }
        };
    }

    /**
     * Genererer ein ny livssyklus som kun blir kalla etter at tidsseriegenereringa er fullført og som delegerer kallet
     * til ein annan funksjon.
     *
     * @param other den andre funksjonen som skal notifiserast om at tidsseriegenereringa er fullført
     * @return ein ny livssyklus
     */
    static TidsserieLivssyklus onStop(final Runnable other) {
        return new TidsserieLivssyklus() {
            @Override
            public void stop(final ServiceRegistry registry) {
                other.run();
            }
        };
    }
}
