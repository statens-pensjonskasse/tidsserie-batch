package no.spk.felles.tidsserie.batch.backend.hazelcast;

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
     * {@link HazelcastBackend#uploader()}.
     *
     * @return masternoda
     */
    HazelcastInstance start();

    /**
     * Terminerer master- og slavenodene umiddelbart.
     * <br>
     * Etter at metoda returnerer skal alt av Hazelcast-noder, trådar og andre ressursar ha blitt lukka.
     */
    void stop();
}
