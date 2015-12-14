package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import com.hazelcast.core.HazelcastInstance;

/**
 * {@link Server} representerer ein strategi for oppstart og køyring av Hazelcast-noder
 * som kan benyttast som mottakar av opplasta medlemsdata og generator av tidsseriar basert
 * på desse medlemsdatane og ymse andre påkrevde referansedata.
 *
 * @author Tarjei Skorgenes
 */
interface Server {
    /**
     * Startar opp master- og slavenodene.
     * <br>
     * Etter at metoda returnerer vil gridet vere klart til å behandle og ta vare på data opplasta via
     * {@link no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend#uploader()}.
     *
     * @return masternoda
     */
    HazelcastInstance start();

    /**
     * Registrerer tenesta angitt via <code>service</code> under tenestenavnet angitt av <code>serviceType</code>
     * i master- og slavenodenes {@link com.hazelcast.core.HazelcastInstance#getUserContext() usercontext}.
     *
     * @param <T>         tenestetypen som blir registrert
     * @param serviceType kva tenestetype tenesta skal registrerast som. Det forventast at tenesta kan castast til
     *                    denne typen av klientane som slår den opp frå usercontexten seinare
     * @param service     tenesta som skal registrerast under det angitte tenestenavnet i usercontexten til alle nodene
     */
    <T> void registrer(Class<T> serviceType, T service);
}
