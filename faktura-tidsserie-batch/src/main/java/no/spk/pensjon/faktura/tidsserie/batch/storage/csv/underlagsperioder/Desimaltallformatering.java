package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    String formater(final double value, final int antallDesimaler) {
        return desimalFormat(antallDesimaler).format(value);
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
}
