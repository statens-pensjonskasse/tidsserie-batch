package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.avregning;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link Desimaltallformatering} representerer ei trådsikker, låsfri teneste for formatering av
 * desimaltall.
 * <br>
 * Tenesta benyttar seg av {@link NumberFormat} som er ikkje er trådsikker og dermed ikkje kan
 * delast på tvers av trådar. Men ein ønskjer likevel å cache formata som blir oppretta, med eit format
 * pr type formatering basert på antall desimalar som krevest.
 * <br>
 * For å unngå deling/låsing av NumberFormat genererer derfor tenesta ein cache med eit NumberFormat pr antall desimalar
 * pr tråd.
 * <br>
 * I tillegg til å cache formata, tar klassa og vare på alle desimalverdiar og deira ferdig formaterte representasjon,
 * pr antall desimalar. Dette blir gjort av ytelsesmessige årsaker ettersom det er observert at eit latterlig stort antall
 * av verdiane som endar opp i tidsserien, er like.
 *
 * @author Tarjei Skorgenes
 * @since 1.2.0
 */
class Desimaltallformatering {
    private final ThreadLocal<Map<Integer, NumberFormat>> desimalformat = ThreadLocal.withInitial(HashMap::new);
    private final ThreadLocal<Map<Key, String>> formatertCache = ThreadLocal.withInitial(WeakHashMap::new);

    String formater(final double verdi, final int antallDesimaler) {
        return formatertCache.get().computeIfAbsent(new Key(verdi, antallDesimaler), k -> desimalFormat(k.desimaler).format(k.verdi));
    }

    NumberFormat desimalFormat(final int antallDesimaler) {
        return desimalformat.get().computeIfAbsent(antallDesimaler, antall -> {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setGroupingUsed(false);
            format.setRoundingMode(RoundingMode.HALF_UP);
            format.setMaximumFractionDigits(antall);
            format.setMinimumFractionDigits(antall);
            return format;
        });
    }

    /**
     * Klasse som omslutter verdi og disimaler, slik at disse kan brukes som nøkkel i formateringscache.
     */
    private static class Key {
        private double verdi;
        private int desimaler;

        public Key(double verdi, int desimaler) {
            this.verdi = verdi;
            this.desimaler = desimaler;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (Double.compare(key.verdi, verdi) != 0) return false;
            return desimaler == key.desimaler;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(verdi);
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + desimaler;
            return result;
        }
    }
}
