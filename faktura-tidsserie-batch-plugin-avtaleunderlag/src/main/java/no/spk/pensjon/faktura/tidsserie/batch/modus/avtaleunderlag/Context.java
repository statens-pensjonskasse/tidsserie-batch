package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<String, Integer> meldinger = new HashMap<>();

    void emit(final String melding, final Integer teller) {
        int akkumulert = 0;
        if (requireNonNull(teller, "teller er påkrevd, men var null") <= 0) {
            throw new IllegalArgumentException(
                    "telleren kan ikke være mindre enn 1"
            );
        }
        if (meldinger.get(requireNonNull(melding, "melding er påkrevd, men var null")) != null) {
            akkumulert = meldinger.get(melding);
            meldinger.put(melding, akkumulert + teller);
        } else {
            meldinger.put(melding, teller);
        }
    }

    public Map<String, Integer> resultat() {
        return meldinger;
    }
}
