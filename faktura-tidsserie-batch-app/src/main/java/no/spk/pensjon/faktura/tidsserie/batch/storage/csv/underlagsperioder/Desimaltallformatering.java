package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link Desimaltallformatering} representerer ei tr�dsikker, l�sfri teneste for formatering av
 * desimaltall.
 * <br>
 * Tenesta benyttar seg av {@link NumberFormat} som er ikkje er tr�dsikker og dermed ikkje kan
 * delast p� tvers av tr�dar. Men ein �nskjer likevel � cache formata som blir oppretta, med eit format
 * pr type formatering basert p� antall desimalar som krevest.
 * <br>
 * For � unng� deling/l�sing av NumberFormat genererer derfor tenesta ein cache med eit NumberFormat pr antall desimalar
 * pr tr�d.
 *
 * @author Tarjei Skorgenes
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
            format.setRoundingMode(RoundingMode.HALF_UP);
            format.setMaximumFractionDigits(antall);
            format.setMinimumFractionDigits(antall);
            return format;
        });
    }

    /**
     * Klasse som omslutter verdi og disimaler, slik at disse kan brukes som n�kkel i formateringscache.
     */
    private static class Key{
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
