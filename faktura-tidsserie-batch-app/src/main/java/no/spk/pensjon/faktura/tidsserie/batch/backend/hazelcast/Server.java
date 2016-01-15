package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import com.hazelcast.core.HazelcastInstance;

/**
 * {@link Server} representerer ein strategi for oppstart og køyring av Hazelcast-noder
 * som kan benyttast som mottakar av opplasta medlemsdata og generator av tidsseriar basert
 * på desse medlemsdatane og ymse andre påkrevde referansedata.
 *
 * @author Tarjei Skorgenes
 */
public interface Server {
    /**
     * Startar opp master- og slavenodene.
     * <br>
     * Etter at metoda returnerer vil gridet vere klart til å behandle og ta vare på data opplasta via
     * {@link no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.HazelcastBackend#uploader()}.
     *
     * @return masternoda
     */
    HazelcastInstance start();
}
